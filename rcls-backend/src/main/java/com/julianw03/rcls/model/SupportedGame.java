package com.julianw03.rcls.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Getter
public enum SupportedGame {
    VALORANT("valorant", "Valorant"),
    LEGENDS_OF_RUNETERRA("bacon", "Legends of Runeterra"),
    LEAGUE_OF_LEGENDS("league_of_legends", "League of Legends"),
    TWO_XKO("lion", "2XKO");

    private final String riotInternalName;
    private final String displayName;

    SupportedGame(String riotInternalName, String displayName) {
        this.riotInternalName = riotInternalName;
        this.displayName = displayName;
    }

    public enum ResolveStrategy {
        RCLS_INTERNAL_NAME(
                (internalName) -> {
                    for (SupportedGame game : SupportedGame.values()) {
                        if (game.name().equalsIgnoreCase(internalName)) return Optional.of(game);
                    }
                    return Optional.empty();
                },
                SupportedGame::name),
        RIOT_INTERNAL_NAME(
                (riotName) -> {
                    for (SupportedGame game : SupportedGame.values()) {
                        if (game.getRiotInternalName().equals(riotName)) return Optional.of(game);
                    }
                    return Optional.empty();
                },
                SupportedGame::getRiotInternalName),
        DISPLAY_NAME(
                (displayName) -> {
                    for (SupportedGame game : SupportedGame.values()) {
                        if (game.getDisplayName().equalsIgnoreCase(displayName)) return Optional.of(game);
                    }
                    return Optional.empty();
                },
                SupportedGame::getDisplayName),
        ;

        private final Function<String, Optional<SupportedGame>> resolver;
        private final Function<SupportedGame, String>           idResolver;

        ResolveStrategy(Function<String, Optional<SupportedGame>> resolver, Function<SupportedGame, String> idResolver) {
            this.resolver = resolver;
            this.idResolver = idResolver;
        }

        public Optional<SupportedGame> resolve(String gameId) {
            log.debug("Resolving gameId {} with strategy {}", gameId, this.name());
            return resolver.apply(gameId);
        }

        public String getId(SupportedGame game) {
            return idResolver.apply(game);
        }

        public static Optional<ResolveStrategy> fromString(String resolveStrategyName) {
            if (resolveStrategyName == null || resolveStrategyName.isEmpty()) return Optional.of(getDefault());
            for (ResolveStrategy strategy : ResolveStrategy.values()) {
                if (strategy.name().equalsIgnoreCase(resolveStrategyName)) return Optional.of(strategy);
            }
            return Optional.empty();
        }

        public static ResolveStrategy getDefault() {
            return ResolveStrategy.RIOT_INTERNAL_NAME;
        }
    }
}
