package com.julianw03.rcls.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class RsoAuthAuthorizationResponse {
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Authorization {
        @Getter
        @NoArgsConstructor
        public static class AccessToken {
            private long         expiry;
            private List<String> scopes;
            private String       token;
        }

        @Getter
        @NoArgsConstructor
        @ToString
        public static class IdToken {
            private long   expiry;
            private String token;
        }

        private AccessToken accessToken;
        private IdToken     idToken;
        private String      isDPoPBound;
    }

    public enum Type {
        @JsonProperty("needs_authentication")
        NEEDS_AUTHENTICATION,
        @JsonProperty("needs_reauthentication")
        NEEDS_REAUTHENTICATION,
        @JsonProperty("authorized")
        AUTHORIZED
    }

    private Authorization authorization;
    private String        country;
    private Type          type;
}
