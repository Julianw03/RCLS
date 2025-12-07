package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

public record UpToDateState() implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.UP_TO_DATE;
    }
}
