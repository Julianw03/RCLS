package com.julianw03.rcls.service.modules.rclient.login.model;

import lombok.Builder;

import java.util.List;

@Builder
public record MultifactorInfoDTO(
        String email,
        String method,
        List<String> methods
) {
}
