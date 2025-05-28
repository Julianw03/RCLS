package com.julianw03.rcls.service.base.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PublisherServiceImpl extends PublisherService {

    private final Queue<PublisherMessage<?>> messageQueue    = new ConcurrentLinkedQueue<>();
    private final ExecutorService         executorService = Executors.newSingleThreadExecutor();
    private final AtomicReference<WebSocketSession> singleSession    = new AtomicReference<>(null);
    private final ObjectMapper                      mapper           = new ObjectMapper();

    @Override
    protected void init() {
        executorService.submit(() -> {
            PublisherMessage<?> message;
            WebSocketSession session;
            while (!Thread.currentThread().isInterrupted()) {
                session = singleSession.get();
                if ((message = messageQueue.poll()) == null) continue;
                if (session == null || !session.isOpen()) continue;
                try {
                    TextMessage sendMessage = new TextMessage(mapper.writeValueAsString(message));
                    session.sendMessage(sendMessage);
                } catch (Exception e) {
                    log.error("Failed to send message", e);
                }
            }
        });
    }

    @Override
    protected void destroy() {
        log.info("Shutdown called");
        long start = System.currentTimeMillis();
        messageQueue.clear();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            log.error("Graceful shutdown failed", e);
            executorService.shutdownNow();
        }
        long end = System.currentTimeMillis();
        log.info("Shutdown succeeded after {}ms", (end - start));
    }

    @Override
    protected void doDispatchChange(String service, String dataType, PublisherFormat data) {
        PublisherMessage<?> message = new PublisherMessage<>(service, dataType, data);
        messageQueue.offer(message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("Session established connection {}", session.getId());
        if (singleSession.compareAndSet(null, session)) {
            log.debug("Only one session is connected, no special handling required");
            return;
        }
        //Set didn't succeed, therefore we already have a session.
        //We will now replace the previous session with the new one
        log.debug("A session is already established, will call updateSessions");
        updateSessions(session);
        super.afterConnectionEstablished(session);
    }

    private synchronized void updateSessions(WebSocketSession newSession) {
        WebSocketSession previousSession = singleSession.getAndSet(newSession);
        if (previousSession != null) {
            log.debug("Previous Session {} exists, will attempt to close it", previousSession.getId());
            if (previousSession.isOpen()) {
                try {
                    previousSession.close(CloseStatus.GOING_AWAY);
                } catch (Exception e) {
                    log.error("Error closing", e);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        singleSession.compareAndSet(session, null);
        log.debug("Connection {} closed with status code {}", session.getId(), status.getCode());
        super.afterConnectionClosed(session, status);
    }
}
