package com.julianw03.rcls.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.eventBus.impl.MultiChannelBusImpl;
import com.julianw03.rcls.eventBus.model.Channel;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.eventBus.model.events.RCUMessageEvent;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.data.ObjectDataManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = DataManagerEventBusTest.TestConfig.class)
public class DataManagerEventBusTest {
    @MockitoBean
    RiotClientService riotClientService;

    @Autowired
    ObjectDataManager<Integer, Integer> testObjectDataManager;

    @Autowired
    MultiChannelBus multiChannelBus;

    private static final Pattern examplePattern = Pattern.compile("^/example/v1/data$");

    private static final Function<Integer, Integer> mapper = Function.identity();


    @SpringBootConfiguration
    static class Dummy {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        MultiChannelBus multiChannelBus() {
            return new MultiChannelBusImpl();
        }

        @Bean
        ObjectDataManager<Integer, Integer> testObjectDataManager(
                RiotClientService riotClientService,
                MultiChannelBus multiChannelBus
        ) {
            return new ObjectDataManager<>(
                    riotClientService,
                    multiChannelBus
            ) {
                @Override
                protected Integer mapView(Integer state) {
                    return Optional.ofNullable(state)
                                   .map(mapper)
                                   .orElse(null);
                }

                @Override
                protected CompletableFuture<Integer> doFetchInitialData() {
                    return CompletableFuture.completedFuture(42);
                }

                @Override
                protected Matcher getUriMatcher(String uri) {
                    return examplePattern.matcher(uri);
                }

                @Override
                protected void handleUpdate(
                        RCUWebsocketMessage.MessageType type,
                        JsonNode data,
                        Matcher uriMatcher
                ) {
                    if (!data.isInt()) throw new IllegalArgumentException("Data should be an integer for testing");
                    setState(data.intValue());
                }
            };
        }
    }


    @Test
    void test_relevantMessageIsReceived() {
        final int initialState = 10;
        final int updatedState = 20;
        testObjectDataManager.setState(initialState);

        final CountDownLatch latch = new CountDownLatch(1);

        multiChannelBus.getFlux(
                               Channel.RCU_PROXY,
                               RCUMessageEvent.class
                       )
                       .doOnNext(e -> latch.countDown())
                       .subscribe();

        multiChannelBus.publish(
                Channel.RCU_PROXY,
                new RCUMessageEvent(
                        "rcu",
                        new RCUWebsocketMessage(
                                RCUWebsocketMessage.MessageType.UPDATE,
                                "/example/v1/data",
                                new ObjectMapper().valueToTree(updatedState)
                        )
                )
        );


        assertTrue(
                assertDoesNotThrow(() -> latch.await(
                        1,
                        TimeUnit.SECONDS
                )),
                "Event should be received within 1 second"
        );

        Integer viewState = assertDoesNotThrow(
                () -> testObjectDataManager.getView(),
                "Getting view state should not throw"
        );

        assertEquals(
                mapper.apply(updatedState),
                viewState,
                "View state should be updated state passed through mapper"
        );
    }
}
