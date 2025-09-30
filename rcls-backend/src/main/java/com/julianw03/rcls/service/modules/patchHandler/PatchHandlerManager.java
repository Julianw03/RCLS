package com.julianw03.rcls.service.modules.patchHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.eventBus.model.MultiChannelBus;
import com.julianw03.rcls.generated.api.PluginPatchProxyApi;
import com.julianw03.rcls.generated.api.PluginRnetProductRegistryApi;
import com.julianw03.rcls.generated.model.PatchProxyPatchingResource;
import com.julianw03.rcls.generated.model.RnetProductRegistryPatchline;
import com.julianw03.rcls.generated.model.RnetProductRegistryProductV4;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.model.data.PublishingMapDataManager;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PatchHandlerManager extends PublishingMapDataManager<String, Map<String, PatchProxyPatchingResource>, Map<String, PatchProxyPatchingResource>> {

    public PatchHandlerManager(
            RiotClientService riotClientService,
            MultiChannelBus eventBus
    ) {
        super(
                riotClientService,
                eventBus
        );
    }

    private static final Pattern PATCH_PROXY_PATTERN = Pattern.compile("^/patch-proxy/v1/patch-states/products/(\\w+)/patchlines/(\\w+)$");

    @Override
    protected CompletableFuture<Map<String, Map<String, PatchProxyPatchingResource>>> doFetchInitialData() {
        log.info("Fetching initial data...");
        return CompletableFuture.supplyAsync(
                () -> {
                    PluginRnetProductRegistryApi rnetProductRegistryApi = riotClientService.getApi(PluginRnetProductRegistryApi.class)
                                                                                           .orElseThrow();

                    List<RnetProductRegistryProductV4> products = rnetProductRegistryApi.rnetProductRegistryV4ProductsGet();

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

                    final Map<String, Map<String, PatchProxyPatchingResource>> patches = new ConcurrentHashMap<>();

                    productMap.keySet().parallelStream().forEach(productId -> {
                        final Map<String, PatchProxyPatchingResource> productPatches = new HashMap<>();
                        for (String patchLine : productMap.get(productId)) {
                            log.debug(
                                    "Fetching initial data for {} {}",
                                    productId,
                                    patchLine
                            );
                            final PatchProxyPatchingResource patchProxyPatchingResource = handleInitialSet(
                                    productId,
                                    patchLine
                            );

                            if (patchProxyPatchingResource == null) continue;

                            productPatches.put(
                                    patchLine,
                                    patchProxyPatchingResource
                            );
                        }
                        patches.put(
                                productId,
                                productPatches
                        );
                    });


                    return patches;
                }
        );
    }

    @Override
    protected void onStateUpdated(
            Map<String, Map<String, PatchProxyPatchingResource>> previousState,
            Map<String, Map<String, PatchProxyPatchingResource>> newState
    ) {
        log.info("Updating entire patch proxy state");
        super.onStateUpdated(
                previousState,
                newState
        );
    }

    private PatchProxyPatchingResource handleInitialSet(
            String productId,
            String patchlineId
    ) {
        PluginPatchProxyApi patchProxyApi = riotClientService.getApi(PluginPatchProxyApi.class)
                                                             .orElseThrow();

        PatchProxyPatchingResource patchProxyPatchingResource = null;
        try {
            patchProxyPatchingResource = patchProxyApi.patchProxyV1PatchStatesProductsProductIdPatchlinesPatchlineIdGet(
                    productId,
                    patchlineId
            );
        } catch (Exception e) {
            log.error(
                    "Failed to fetch initial patch proxy data for productId: {}, patchlineId: {}",
                    productId,
                    patchlineId,
                    e
            );
        }

        return patchProxyPatchingResource;
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

        log.info(
                "Updating patch proxy with productId: {}, patchlineId: {}",
                productId,
                patchlineId
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

                if (!this.initialFetchDone.get()) {
                    log.info("Initial fetch not done yet, skipping update");
                    return;
                }

                PatchProxyPatchingResource patchlineResource = resource.get();
                putPatchline(
                        productId,
                        patchlineId,
                        patchlineResource
                );
            }
            case DELETE -> {
                removePatchline(
                        productId,
                        patchlineId
                );
            }
            case null, default -> {
                log.warn(
                        "Unsupported patch proxy type: {}",
                        type
                );
            }
        }
    }

    synchronized void putPatchline(
            String productId,
            String patchlineId,
            PatchProxyPatchingResource resource
    ) {
        Map<String, PatchProxyPatchingResource> patchlines = get(productId);
        if (patchlines == null) {
            patchlines = new HashMap<>();
        } else {
            patchlines = new HashMap<>(patchlines);
        }

        patchlines.put(
                patchlineId,
                resource
        );

        put(
                productId,
                patchlines
        );
    }

    synchronized void removePatchline(
            String productId,
            String patchlineId
    ) {
        Map<String, PatchProxyPatchingResource> patchlines = get(productId);
        if (patchlines == null) {
            log.warn(
                    "No patchlines found for productId: {}",
                    productId
            );
            return;
        }

        patchlines = new HashMap<>(patchlines);
        patchlines.remove(patchlineId);

        if (patchlines.isEmpty()) {
            remove(productId);
        } else {
            put(
                    productId,
                    patchlines
            );
        }
    }

    @Override
    protected Map<String, PatchProxyPatchingResource> mapValueView(Map<String, PatchProxyPatchingResource> value) {
        return Collections.unmodifiableMap(value);
    }
}
