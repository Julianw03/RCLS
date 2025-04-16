package com.julianw03.rcls.model.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class RsoAuthenticatorV1RiotIdentityAuthCompleteInput {
    private String  campaign;
    private String  captcha;
    private String  language;
    private String  password;
    private Boolean remember;
    private String  username;
}
