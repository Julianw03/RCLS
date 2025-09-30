package com.julianw03.rcls.service.base.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.data.ObjectDataManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ObjectDataManagerTest {

    @Mock
    RiotClientService riotClientService;

    private ObjectDataManager<Integer, Integer> objectDataManager;
    private Pattern                             examplePattern = Pattern.compile("^/example/v1/data$");

    private final Function<Integer, Integer> mapper = state -> state + 1;

    @BeforeEach
    void setUp() {
        objectDataManager = new ObjectDataManager<>(riotClientService) {
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

            }
        };
    }

    @Test
    void test_mapViewGetsApplied() {
        final int initialState = 10;
        objectDataManager.setState(initialState);
        Integer viewState = assertDoesNotThrow(
                () -> objectDataManager.getView(),
                "Getting view state should not throw"
        );
        assertEquals(
                viewState,
                mapper.apply(initialState),
                "View state should be initial state passed through mapper"
        );
    }

    @Test
    void test_resettingStateSetsViewToNull() {
        final int initialState = 10;
        objectDataManager.setState(initialState);
        objectDataManager.reset();
        Integer viewState = assertDoesNotThrow(
                () -> objectDataManager.getView(),
                "Getting view state should not throw"
        );
        assertNull(
                viewState,
                "View state should be null after reset"
        );
    }

    @Test
    void test_relevantMessageGetsPassed() {

    }
}
