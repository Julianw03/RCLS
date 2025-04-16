package com.julianw03.rcls.model.security;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class EncryptedDataDeserializer extends JsonDeserializer<EncryptedData> {

    @Override
    public EncryptedData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.getCodec().readTree(p);
        final String base64Salt = node.get("base64Salt").textValue();
        final String base64Data = node.get("base64Data").textValue();

        return new EncryptedData(base64Salt, base64Data);

    }
}
