package com.julianw03.rcls.service.modules.rclient.patchHandler.model;

import com.julianw03.rcls.model.services.DiscriminatorEnum;

public enum PatchlineStateDTO implements DiscriminatorEnum {
    UNKNOWN,
    AWAITING_PATCH_DATA,
    UP_TO_DATE,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUIRED,
    NOT_INSTALLED,
    REPAIR_IN_PROGRESS,
    ERROR
}
