package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

public record OutOfDateState(
        Info info
) implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.UPDATE_REQUIRED;
    }

    public record Info(
            boolean pausedByUser
    ) {
    }
}
