package com.julianw03.rcls.service.modules.rclient.patchHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.generated.api.PluginPatchProxyApi;
import com.julianw03.rcls.generated.api.PluginRnetProductRegistryApi;
import com.julianw03.rcls.generated.model.*;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.SupportedGame;
import com.julianw03.rcls.model.data.PublishingMapDataManager;
import com.julianw03.rcls.service.modules.rclient.patchHandler.model.CompositePatchlineKey;
import com.julianw03.rcls.service.modules.rclient.patchHandler.model.PatchStateDTO;
import com.julianw03.rcls.service.modules.rclient.patchHandler.model.states.*;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PatchHandlerManager extends PublishingMapDataManager<CompositePatchlineKey, PatchProxyPatchingResource, PatchState, PatchStateDTO> {

    private static final Pattern PATCH_PROXY_PATTERN = Pattern.compile("^/patch-proxy/v1/patch-states/products/(\\w+)/patchlines/(\\w+)$");

    public PatchHandlerManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
    }

    @Override
    protected PatchStateDTO mapPublishingValueView(PatchProxyPatchingResource state) {
        return PatchStateDTO.map(mapValueView(state));
    }

    @Override
    protected PatchState mapValueView(PatchProxyPatchingResource value) {
        if (value == null) return new UnknownState();
        switch (value.getCombinedPatchState()) {
            case UP_TO_DATE -> {
                return new UpToDateState();
            }
            case UPDATING -> {
                PatchProxyPatchStatus status = value.getPatchStatus();
                final double totalProgressPercent = Optional.ofNullable(status.getProgress()
                                                                              .getProgress())
                                                            .orElse(0.0);
                switch (status.getState()) {
                    case REPAIRING -> {
                        return Optional.ofNullable(status.getProgress())
                                       .map(PatchProxyProgress::getRepair)
                                       .map(repairState -> new RepairInProgressState.Progress(
                                               totalProgressPercent,
                                               repairState.getBytesToRepair(),
                                               repairState.getFilesToRepair(),
                                               repairState.getRepairedBytes(),
                                               repairState.getRepairedFiles()
                                       ))
                                       .map(progress -> (PatchState) new RepairInProgressState(progress))
                                       .orElseGet(() -> {
                                           log.warn("Repair progress is null despite being in REPAIRING state");
                                           return RepairInProgressState.ZERO_PROGRESS;
                                       });
                    }
                    case UPDATING -> {
                        return Optional.ofNullable(status.getProgress())
                                       .map(PatchProxyProgress::getUpdate)
                                       .map(updateState -> new UpdateInProgressState.Progress(
                                               totalProgressPercent,
                                               updateState.getBytesToDownload(),
                                               updateState.getBytesToRead(),
                                               updateState.getBytesToWrite(),
                                               updateState.getDownloadedBytes(),
                                               updateState.getReadBytes(),
                                               updateState.getStage(),
                                               updateState.getWrittenBytes()
                                       ))
                                       .map(progress -> (PatchState) new UpdateInProgressState(progress))
                                       .orElseGet(() -> {
                                           log.warn("Update progress is null despite being in UPDATING state");
                                           return UpdateInProgressState.ZERO_PROGRESS;
                                       });
                    }
                    case null, default -> {
                        log.warn(
                                "Unknown updating state {}",
                                status.getState()
                        );
                        return new UnknownState();
                    }
                }
            }
            case NOT_INSTALLED -> {
                return new NotInstalledState();
            }
            case OUT_OF_DATE -> {
                return new OutOfDateState(
                        new OutOfDateState.Info(
                                Optional.ofNullable(value.getUserCancelledPatching())
                                        .orElse(false)
                        ));
            }
            case AWAITING_HEADERS -> {
                return new AwaitingPatchDataState();
            }
            case null, default -> {
                log.debug(
                        "Unsupported State {}",
                        value.getCombinedPatchState()
                );
                return new UnknownState();
            }
        }
    }

    @Override
    protected CompletableFuture<Map<CompositePatchlineKey, PatchProxyPatchingResource>> doFetchInitialData() {
        log.info("Fetching initial data...");
        return CompletableFuture.supplyAsync(this::getProducts)
                                .orTimeout(
                                        5,
                                        TimeUnit.SECONDS
                                )
                                .thenApply(products -> {
                                    final Map<String, Set<String>> productMap = products.stream()
                                                                                        .collect((Collectors.toMap(
                                                                                                RnetProductRegistryProductV4::getId,
                                                                                                product -> product.getPatchlines()
                                                                                                                  .stream()
                                                                                                                  .filter(Objects::nonNull)
                                                                                                                  .filter(patchline -> {
                                                                                                                      final String platform = patchline.getPlatform();
                                                                                                                      return Optional.ofNullable(patchline.getAvailablePlatforms())
                                                                                                                                     .map(availablePlatforms -> availablePlatforms.contains(platform))
                                                                                                                                     .orElse(false);
                                                                                                                  })
                                                                                                                  .map(RnetProductRegistryPatchline::getId)
                                                                                                                  .collect(Collectors.toSet())
                                                                                        )));


                                    return productMap.entrySet()
                                                     .parallelStream()
                                                     .flatMap(entry -> SupportedGame.ResolveStrategy.RIOT_INTERNAL_NAME.resolve(entry.getKey())
                                                                                                                       .stream()
                                                                                                                       .flatMap(supportedGame -> entry.getValue()
                                                                                                                                                      .stream()
                                                                                                                                                      .map(patchlineId -> byPatchlineIdAndSupportedGame(
                                                                                                                                                              patchlineId,
                                                                                                                                                              supportedGame
                                                                                                                                                      ))
                                                                                                                                                      .filter(Objects::nonNull)
                                                                                                                       )
                                                     )
                                                     .collect(Collectors.toMap(
                                                             data -> data.key,
                                                             data -> data.resource
                                                     ));

                                });
    }

    private List<RnetProductRegistryProductV4> getProducts() {
        PluginRnetProductRegistryApi rnetProductRegistryApi = riotClientService.getApi(PluginRnetProductRegistryApi.class)
                                                                               .orElseThrow();

        return rnetProductRegistryApi.rnetProductRegistryV4ProductsGet();
    }

    private Optional<PatchProxyPatchingResource> handleInitialDataFetch(
            CompositePatchlineKey key
    ) {
        PluginPatchProxyApi patchProxyApi = riotClientService.getApi(PluginPatchProxyApi.class)
                                                             .orElseThrow();

        log.trace(
                "Fetching initial patch proxy data for key: {}",
                key
        );
        PatchProxyPatchingResource patchProxyPatchingResource = null;
        try {
            patchProxyPatchingResource = patchProxyApi.patchProxyV1PatchStatesProductsProductIdPatchlinesPatchlineIdGet(
                    key.game()
                       .getRiotInternalName(),
                    key.patchlineId()
            );
            log.debug(
                    "Fetched initial patch proxy data for key {}",
                    key
            );
        } catch (Exception e) {
            log.warn(
                    "Failed to fetch initial patch proxy data for key: {}: {}",
                    key,
                    e
            );
        }

        return Optional.ofNullable(patchProxyPatchingResource);
    }


    private CollectablePatchData byPatchlineIdAndSupportedGame(
            String patchlineId,
            SupportedGame game
    ) {
        CompositePatchlineKey key = new CompositePatchlineKey(
                game,
                patchlineId
        );

        return handleInitialDataFetch(key)
                .map(resource -> new CollectablePatchData(
                        key,
                        resource
                ))
                .orElse(null);
    }

    @Override
    protected Matcher getUriMatcher(String uri) {
        return PATCH_PROXY_PATTERN.matcher(uri);
    }

    @Override
    protected void handleUpdate(
            RCUWebsocketMessage.MessageType type,
            JsonNode data,
            Matcher uriMatcher
    ) {

        if (uriMatcher.groupCount() < 2) {
            log.warn(
                    "URI matcher did not match expected groups: {}",
                    uriMatcher
            );
            return;
        }

        final String productId = uriMatcher.group(1);
        final String patchlineId = uriMatcher.group(2);

        Optional<SupportedGame> optGame = SupportedGame.ResolveStrategy.RIOT_INTERNAL_NAME.resolve(productId);

        if (optGame.isEmpty()) {
            log.warn(
                    "Unsupported productId received in patch proxy update: {}",
                    productId
            );
            return;
        }

        final CompositePatchlineKey key = new CompositePatchlineKey(
                optGame.get(),
                patchlineId
        );

        log.debug(
                "Updating patch proxy with CompositePatchlineKey: {}",
                key
        );

        switch (type) {
            case UPDATE, CREATE -> {
                Optional<PatchProxyPatchingResource> resource = parseJson(
                        data,
                        PatchProxyPatchingResource.class
                );
                if (resource.isEmpty()) {
                    log.warn(
                            "Failed to parse PatchProxyPatchingResource from data: {}",
                            data
                    );
                    return;
                }

                PatchProxyPatchingResource patchlineResource = resource.get();
                put(
                        key,
                        patchlineResource
                );
            }
            case DELETE -> {
                remove(key);
            }
            case null, default -> {
                log.warn(
                        "Unsupported patch proxy type: {}",
                        type
                );
            }
        }
    }


    private record CollectablePatchData(CompositePatchlineKey key, PatchProxyPatchingResource resource) {
    }
}
