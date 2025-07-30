package com.julianw03.rcls.service.base.publisher.formats;

import com.julianw03.rcls.service.base.riotclient.RiotClientService;

public record RCConnectionStateFormat(
        RiotClientService.SimpleConnectionState connectionState
) implements PublisherFormat {}

