package com.julianw03.rcls.service.modules.rclient.patchHandler.model;

import com.julianw03.rcls.model.services.ServiceDTO;
import com.julianw03.rcls.service.modules.rclient.patchHandler.model.states.*;
import lombok.Builder;
import org.jetbrains.annotations.Contract;

@Builder
public record PatchStateDTO(
        PatchlineStateDTO patchlineState,
        RepairInProgressState.Progress repairProgress,
        UpdateInProgressState.Progress updateProgress,
        OutOfDateState.Info outOfDateInfo,
        ErrorState.InfoDTO errorInfo
) implements ServiceDTO<PatchState> {
    @Contract(pure = true)
    public static PatchStateDTO map(PatchState state) {
        final PatchStateDTOBuilder builder = new PatchStateDTOBuilder();
        builder.patchlineState(state.getDiscriminator());
        switch (state) {
            case ErrorState errorState -> {
                builder.errorInfo(errorState.info());
            }
            case OutOfDateState outOfDateState -> {
                builder.outOfDateInfo(outOfDateState.info());
            }
            case RepairInProgressState repairInProgressState -> {
                builder.repairProgress(repairInProgressState.repairProgress());
            }
            case UpdateInProgressState updateInProgressState -> {
                builder.updateProgress(updateInProgressState.updateProgress());
            }
            default -> {

            }
        }
        return builder.build();
    }
}
