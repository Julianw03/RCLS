package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

import java.math.BigDecimal;

public record UpdateInProgressState(
        Progress updateProgress
) implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.UPDATE_IN_PROGRESS;
    }

    public record Progress(
            double totalProgressPercentage,
            BigDecimal bytesToDownload,
            BigDecimal bytesToRead,
            BigDecimal bytesToWrite,
            BigDecimal downloadedBytes,
            BigDecimal readBytes,
            BigDecimal stage,
            BigDecimal writtenBytes
    ) {

    }

    public static final UpdateInProgressState ZERO_PROGRESS = new UpdateInProgressState(
            new Progress(
                    0.0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
}
