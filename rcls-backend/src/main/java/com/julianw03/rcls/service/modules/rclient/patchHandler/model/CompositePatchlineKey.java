package com.julianw03.rcls.service.modules.rclient.patchHandler.model;

import com.julianw03.rcls.model.SupportedGame;
import org.jetbrains.annotations.NotNull;

public record CompositePatchlineKey(SupportedGame game, String patchlineId) {
    @Override
    public @NotNull String toString() {
        return game.name() + "." + patchlineId;
    }
}
