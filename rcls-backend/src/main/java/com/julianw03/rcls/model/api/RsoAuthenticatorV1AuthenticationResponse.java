package com.julianw03.rcls.model.api;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class RsoAuthenticatorV1AuthenticationResponse {
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Details {
        private String auth_method;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Captcha {
        @Getter
        @NoArgsConstructor
        public static class HCaptcha {
            private String data;
            private String key;
        }

        public enum Type {
            @JsonProperty("none")
            NONE,
            @JsonProperty("hcaptcha")
            HCAPTCHA
        }

        private HCaptcha hcaptcha;
        private Type     type;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class GamepassResponseDetails {
        private int            delay;
        private int            remaining;
        private GamepassStatus status;

        public enum GamepassStatus {
            @JsonProperty("PENDING")
            PENDING,
            @JsonProperty("ACTIVE")
            ACTIVE,
            @JsonProperty("NONE")
            NONE
        }
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Healup {
        private String       auth_method;
        private List<String> required_fields;
        private ObjectNode   required_fields_hints;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class MultifactorDetails {
        private String       auth_method;
        private String       email;
        private String       known_value;
        private String       method;
        private List<String> methods;
        private String       mode;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Success {
        private String  auth_method;
        private Boolean is_console_link_session;
        private String  linked;
        private String  login_token;
        private String  puuid;
        private String  redirect_url;
    }

    private Details                 auth;
    private Captcha                 captcha;
    private String                  cluster;
    private String                  country;
    private String                  error;
    private GamepassResponseDetails gamepass;
    private Healup                  healup;
    @JsonProperty("kr-id-verification")
    private JsonNode                krIdVerification;
    private MultifactorDetails      multifactor;
    private JsonNode                signup;
    private Success                 success;
    private String                  suuid;
    private String                  timestamp;
    private String                  type;
    private JsonNode                validation_captcha;
}
