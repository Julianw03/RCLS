package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

import java.math.BigDecimal;

public record RepairInProgressState(
        Progress repairProgress
) implements PatchState {
    @Override
    public PatchlineStateDTO getDiscriminator() {
        return PatchlineStateDTO.REPAIR_IN_PROGRESS;
    }

    public record Progress(
            double totalProgressPercentage,
            BigDecimal bytesToRepair,
            BigDecimal filesToRepair,
            BigDecimal repairedBytes,
            BigDecimal repairedFiles
    ) {

    }

    public static final RepairInProgressState ZERO_PROGRESS = new RepairInProgressState(
            new Progress(
                    0.0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            )
    );
}
