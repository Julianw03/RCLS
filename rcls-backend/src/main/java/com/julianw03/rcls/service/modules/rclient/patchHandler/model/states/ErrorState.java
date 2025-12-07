package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

public record ErrorState(
        InfoDTO info
) implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.ERROR;
    }

    public record InfoDTO(
            String code,
            String message
    ) {
    }
}
