package com.julianw03.rcls.service.modules.rclient.login.model;

import com.julianw03.rcls.model.services.DiscriminatorEnum;

public enum LoginStatusDTO implements DiscriminatorEnum {
    LOGGED_IN,
    LOGGED_OUT,
    MULTIFACTOR_REQUIRED,
    ERROR,
    UNKNOWN
}
