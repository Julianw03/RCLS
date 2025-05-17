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
public class AccountLoginInput {
    private String  campaign;
    private String  captcha;
    private String  language;
    private Boolean remember;

    public RsoAuthenticatorV1RiotIdentityAuthCompleteInput withUserDetails(
            String username,
            String password
    ) {
        final RsoAuthenticatorV1RiotIdentityAuthCompleteInput input = new RsoAuthenticatorV1RiotIdentityAuthCompleteInput();
        input.setCampaign(campaign);
        input.setCaptcha(captcha);
        input.setLanguage(language);
        input.setRemember(remember);
        input.setUsername(username);
        input.setPassword(password);
        return input;
    }
}
