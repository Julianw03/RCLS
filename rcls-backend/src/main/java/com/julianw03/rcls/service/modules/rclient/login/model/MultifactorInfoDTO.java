package com.julianw03.rcls.service.modules.rclient.login.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MultifactorInfoDTO {
    private String email;
    private String method;
    private List<String> methods;
}
