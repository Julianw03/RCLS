package com.julianw03.rcls.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RsoAuthenticatorV1RiotIdentityAuthCompleteInput {
    private String  campaign;
    private String  captcha;
    private String  language;
    private String  password;
    private Boolean remember;
    private String  username;
}
