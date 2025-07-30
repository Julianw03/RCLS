package com.julianw03.rcls.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;

/**
 * We will use an abstract class as this allows us to annotate the {@link  #startup()} and {@link #shutdown()} Methods
 * with {@link jakarta.annotation.PostConstruct} and {@link jakarta.annotation.PreDestroy} respectively
 * */
public abstract class BaseService {
    /**
     * Should be called before all other calls. Used to initialize components in the service.
     * The related service should be assumed to be unusable before this call.
     * The method may be blocking.
     * */
    @PostConstruct
    public void startup() {};
    /**
     * Should be called upon destruction. Used to clean up components in the service.
     * The related service should be assumed to be unusable after this call.
     * The method may be blocking.
     * */
    @PreDestroy
    public void shutdown() {};
}
