package com.julianw03.rcls.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class RsoAuthSessionResponse {
    public enum AuthenticationType {
        @JsonProperty("SSOAuth")
        SSO_AUTH,
        @JsonProperty("RiotAuth")
        RIOT_AUTH,
        @JsonProperty("None")
        NONE
    }

    public enum Type {
        @JsonProperty("unknown_authentication_response")
        UNKNOWN_AUTHENTICATION_RESPONSE,
        @JsonProperty("needs_credentials")
        NEEDS_CREDENTIALS,
        @JsonProperty("needs_password")
        NEEDS_PASSWORD,
        @JsonProperty("needs_multifactor_verification")
        NEEDS_MULTIFACTOR_VERIFICATON,
        @JsonProperty("authenticated")
        AUTHENTICATED,
        @JsonProperty("error")
        ERROR
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class MultifactorDetails {
        public enum Method {
            @JsonProperty("method_not_set")
            METHOD_NOT_SET,
            @JsonProperty("email")
            EMAIL,
            @JsonProperty("authenticator")
            AUTHENTICATOR,
            @JsonProperty("sms")
            SMS,
            @JsonProperty("push")
            PUSH
        }

        private String       email;
        private Method       method;
        private List<String> methods;
        private String       mfaVersion;
        private Integer      multiFactorCodeLength;
    }

    private AuthenticationType authenticationType;
    private String             country;
    private String             error;
    private MultifactorDetails multifactor;
    private Boolean            persistLogin;
    private String             securityProfile;
    private Type               type;
}
