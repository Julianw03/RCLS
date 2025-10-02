package com.julianw03.rcls.model.data;

import java.util.concurrent.CompletableFuture;

public interface DataManagerFacade {
    CompletableFuture<Void> setupInternalState();

    void reset();

    void resetInternalState();
}
