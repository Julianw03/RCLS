package com.julianw03.rcls.service.modules.login.model;

import lombok.Getter;

@Getter
public class MultifactorRequiredException extends Exception {
    private final MultifactorInfoDTO multifactorInfo;

    public MultifactorRequiredException(MultifactorInfoDTO multifactorInfo) {
        super();
        this.multifactorInfo = multifactorInfo;
    }
}
