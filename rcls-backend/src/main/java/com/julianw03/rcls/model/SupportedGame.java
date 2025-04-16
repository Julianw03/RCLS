package com.julianw03.rcls.model;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum SupportedGame {
    VALORANT("valorant"),
    LEGENDS_OF_RUNETERRA("bacon"),
    LEAGUE_OF_LEGENDS("league_of_legends");

    private final String riotInternalName;

    SupportedGame(String riotInternalName) {
        this.riotInternalName = riotInternalName;
    }

    public static Optional<SupportedGame> fromInternalName(String internalName) {
        for (SupportedGame game: SupportedGame.values()) {
            if (game.getRiotInternalName().equals(internalName)) return Optional.of(game);
        }
        return Optional.empty();
    }

    public static Optional<SupportedGame> fromString(String gameId) {
        for (SupportedGame game : SupportedGame.values()) {
            if (game.name().equalsIgnoreCase(gameId)) return Optional.of(game);
        }
        return Optional.empty();
    }
}
