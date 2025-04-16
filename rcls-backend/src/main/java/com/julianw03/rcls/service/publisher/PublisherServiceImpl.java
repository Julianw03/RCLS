package com.julianw03.rcls.service.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.service.publisher.PublisherMessage.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PublisherServiceImpl extends PublisherService {

    private final Queue<TextMessage>                textMessageQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService                   executorService  = Executors.newSingleThreadExecutor();
    private final AtomicReference<WebSocketSession> singleSession    = new AtomicReference<>(null);
    private final ObjectMapper                      mapper           = new ObjectMapper();

    @Override
    protected void init() {
        executorService.submit(() -> {
            TextMessage message;
            WebSocketSession session;
            while (!Thread.currentThread().isInterrupted()) {
                if ((message = textMessageQueue.poll()) == null || (session = singleSession.get()) == null) continue;
                try {
                    session.sendMessage(message);
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
        textMessageQueue.clear();
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
    public void doDispatchChange(Type type, String uri, JsonNode data) {
        PublisherMessage message = new PublisherMessage(type, uri, data);
        final TextMessage sendingMessage = new TextMessage(mapper.valueToTree(message).toString());
        textMessageQueue.offer(sendingMessage);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("Session established connection {}", session.getId());
        if (singleSession.compareAndSet(null, session)) {
            log.debug("Only one session is connected, no special handling required");
            return;
        }
        //Set didn't succeed, therefore we already have a session.
        //We will now replace the previous session with the new one
        log.debug("A session is already established, will call updateSessions");
        updateSessions(session);
    }

    private synchronized void updateSessions(WebSocketSession newSession) {
        WebSocketSession previousSession = singleSession.getAndSet(newSession);
        textMessageQueue.clear();
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
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        singleSession.compareAndSet(session, null);
        log.debug("Connection {} closed with status code {}", session.getId(), status.getCode());
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        log.debug("Received message: {}", message.getPayload());

        // Echo message back to the client
        final WebSocketSession s = singleSession.get();
        if (s.isOpen()) {
            textMessageQueue.offer(new TextMessage(message.getPayload()));
        }
    }

    @Override
    public void subscribe(Flow.Subscriber<? super PublisherMessage> subscriber) {

    }
}
