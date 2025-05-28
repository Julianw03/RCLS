package com.julianw03.rcls.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RCUWebsocketMessage {
    @JsonProperty("eventType")
    private MessageType type;
    private String      uri;
    private JsonNode    data;

    public enum MessageType {
        @JsonProperty("Create")
        CREATE,
        @JsonProperty("Update")
        UPDATE,
        @JsonProperty("Delete")
        DELETE;

        MessageType() {
        }
    }

    protected RCUWebsocketMessage(MessageType type, String uri, JsonNode data) {
        this.type = type;
        this.uri = uri;
        this.data = data;
    }
}
