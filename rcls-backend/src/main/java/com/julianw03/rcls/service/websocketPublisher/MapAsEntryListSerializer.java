package com.julianw03.rcls.service.websocketPublisher;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class MapAsEntryListSerializer extends JsonSerializer<MapEntries<?, ?>> {

    @Override
    public void serialize(
            MapEntries<?, ?> value,
            JsonGenerator gen,
            SerializerProvider serializers
    ) throws IOException {
        gen.writeStartArray();
        for (var e : value.map().entrySet()) {
            gen.writeStartObject();
            gen.writeObjectField("key", e.getKey());
            gen.writeObjectField("value", e.getValue());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
