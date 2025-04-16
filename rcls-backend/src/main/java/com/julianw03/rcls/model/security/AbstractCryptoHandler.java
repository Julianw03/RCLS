package com.julianw03.rcls.model.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

public abstract class AbstractCryptoHandler {
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final   JsonNode     configurationData;

    protected AbstractCryptoHandler() {
        this(null);
    }

    protected AbstractCryptoHandler(JsonNode configurationData) {
        if (configurationData == null || configurationData.isNull()) throw new IllegalArgumentException();
        this.configurationData = configurationData;
    }

    public abstract Optional<String> encrypt(byte[] data, byte[] salt, byte[] password);

    public abstract Optional<String> decrypt(byte[] data, byte[] salt, byte[] password);
}
