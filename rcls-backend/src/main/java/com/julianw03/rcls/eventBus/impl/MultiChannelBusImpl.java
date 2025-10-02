package com.julianw03.rcls.eventBus.impl;

import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.SimpleEvent;
import com.julianw03.rcls.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MultiChannelBusImpl extends BaseService implements MultiChannelBus {

    private final Map<Channel, Sinks.Many<SimpleEvent<?>>> channels = new ConcurrentHashMap<>();

    private Sinks.Many<SimpleEvent<?>> createNewSink(Channel channel) {
        return Sinks.many()
                    .multicast()
                    .onBackpressureBuffer();
    }

    @Override
    public <T extends SimpleEvent<?>> void publish(
            Channel channel,
            T event
    ) {
        if (channel == null || event == null) return;
        emitInSinkSync(
                channel,
                event
        );
        if (channel != Channel.ALL) {
            emitInSinkSync(
                    Channel.ALL,
                    event
            );
        }
    }

    // TODO: Maybe just build a custom adapter for this?
    private void emitInSinkSync(
            Channel channel,
            SimpleEvent<?> event
    ) {
        final Sinks.Many<SimpleEvent<?>> sink = channels.computeIfAbsent(
                channel,
                this::createNewSink
        );
        synchronized (sink) {
            sink.emitNext(
                    event,
                    (SignalType signalType, Sinks.EmitResult emitResult) -> {
                        if (emitResult == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) return false;
                        log.warn("Failed to emit event {} on channel {}: {} ({})", event, channel, signalType, emitResult);
                        return false;
                    }
            );
        }
    }

    @Override
    public <T extends SimpleEvent<?>> Flux<T> getFlux(
            Channel channel,
            Class<T> eventType
    ) {
        return channels.computeIfAbsent(
                               channel,
                               this::createNewSink
                       )
                       .asFlux()
                       .filter(eventType::isInstance)
                       .map(eventType::cast);
    }

    @Override
    public Flux<SimpleEvent<?>> getFlux(
            Channel channel
    ) {
        return channels.computeIfAbsent(
                               channel,
                               this::createNewSink
                       )
                       .asFlux();
    }
}
