package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

public record AwaitingPatchDataState() implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.AWAITING_PATCH_DATA;
    }

}
