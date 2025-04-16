package com.julianw03.rcls.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RsoAuthenticatorV1MultifactorInput {
    private Multifactor multifactor;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Multifactor {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String  action;
        private String  otp;
        private Boolean rememberDevice;
    }
}
