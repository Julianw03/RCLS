package com.julianw03.rcls.service.modules.rclient.patchHandler.model.states;

import com.julianw03.rcls.model.services.DiscriminatedSerializable;
import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchlineStateDTO;

public sealed interface PatchState extends DiscriminatedSerializable<PatchlineStateDTO> permits AwaitingPatchDataState, ErrorState, NotInstalledState, OutOfDateState, RepairInProgressState, UnknownState, UpToDateState, UpdateInProgressState {
}
