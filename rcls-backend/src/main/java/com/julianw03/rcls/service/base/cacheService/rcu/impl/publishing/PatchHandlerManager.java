package com.julianw03.rcls.service.base.cacheService.rcu.impl.publishing;

import com.fasterxml.jackson.databind.JsonNode;
import com.julianw03.rcls.generated.api.CoreSdkApi;
import com.julianw03.rcls.generated.api.PluginRnetProductRegistryApi;
import com.julianw03.rcls.generated.model.PatchProxyPatchingResource;
import com.julianw03.rcls.generated.model.RnetProductRegistryProductV4;
import com.julianw03.rcls.model.RCUWebsocketMessage;
import com.julianw03.rcls.service.base.cacheService.MapDataManager;
import com.julianw03.rcls.service.base.cacheService.StateService;
import com.julianw03.rcls.service.base.cacheService.rcu.RCUStateService;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PatchHandlerManager extends MapDataManager<String, Map<String, PatchProxyPatchingResource>> {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PatchHandlerManager(RiotClientService riotClientService, RCUStateService cacheService) {
        super(riotClientService, cacheService);
    }

    private static final Pattern PATCH_PROXY_PATTERN = Pattern.compile("^/patch-proxy/v1/patch-states/products/(\\w+)/patchlines/(\\w+)$");


    @Override
    protected CompletableFuture<Map<String, Map<String, PatchProxyPatchingResource>>> doFetchInitialData() {
        log.info("Fetching initial data...");
//        return CompletableFuture.supplyAsync(() -> {
//            PluginRnetProductRegistryApi rnetProductRegistryApi = riotClientService.getApi(PluginRnetProductRegistryApi.class).orElseThrow();
//
//            List<RnetProductRegistryProductV4> products = rnetProductRegistryApi.rnetProductRegistryV4ProductsGet();
//
//            products.stream()
//                    .map(RnetProductRegistryProductV4::getPatchlines)
//                    .filter(Objects::nonNull)
//                    .flatMap(List::stream)
//                    .filter(patchline -> {
//                        final String platform = patchline.getPlatform();
//                        return Optional.ofNullable(patchline.getAvailablePlatforms()).map(availablePlatforms -> availablePlatforms.contains(platform)).orElse(false);
//                    })
//                    .forEach(patchline -> {
//                        String
//                    })
//
//
//        }, executor);
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    protected Matcher getUriMatcher(String uri) {
        return PATCH_PROXY_PATTERN.matcher(uri);
    }

    @Override
    protected void handleUpdate(RCUWebsocketMessage.MessageType type, JsonNode data, Matcher uriMatcher) {

        if (uriMatcher.groupCount() < 2) {
            log.warn("URI matcher did not match expected groups: {}", uriMatcher);
            return;
        }

        final String productId = uriMatcher.group(1);
        final String patchlineId = uriMatcher.group(2);

        log.info("Updating patch proxy with productId: {}, patchlineId: {}", productId, patchlineId);

        switch (type) {
            case UPDATE, CREATE -> {
                Optional<PatchProxyPatchingResource> resource = parseJson(data, PatchProxyPatchingResource.class);
                if (resource.isEmpty()) {
                    log.warn("Failed to parse PatchProxyPatchingResource from data: {}", data);
                    return;
                }


                PatchProxyPatchingResource patchlineResource = resource.get();
                putPatchline(productId, patchlineId, patchlineResource);
            }
            case DELETE -> {
                removePatchline(productId, patchlineId);
            }
            case null, default -> {
                log.warn("Unsupported patch proxy type: {}", type);
            }
        }
    }

    synchronized void putPatchline(String productId, String patchlineId, PatchProxyPatchingResource resource) {
        Map<String, PatchProxyPatchingResource> patchlines = get(productId);
        if (patchlines == null) {
            patchlines = new HashMap<>();
        } else {
            patchlines = new HashMap<>(patchlines);
        }

        put(productId, patchlines);
    }

    synchronized void removePatchline(String productId, String patchlineId) {
        Map<String, PatchProxyPatchingResource> patchlines = get(productId);
        if (patchlines == null) {
            log.warn("No patchlines found for productId: {}", productId);
            return;
        }

        patchlines = new HashMap<>(patchlines);
        patchlines.remove(patchlineId);

        if (patchlines.isEmpty()) {
            remove(productId);
        } else {
            put(productId, patchlines);
        }
    }
}
