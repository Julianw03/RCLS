package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

public record UnknownState() implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.UNKNOWN;
    }

    public static final UnknownState INSTANCE = new UnknownState();
}
