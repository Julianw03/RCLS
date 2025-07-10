package com.julianw03.rcls.service.base.cacheService.rcls;

import com.julianw03.rcls.service.base.cacheService.StateService;
import com.julianw03.rcls.service.base.publisher.PublisherService;
import com.julianw03.rcls.service.base.publisher.formats.PublisherFormat;
import org.springframework.stereotype.Service;

@Service
public class RCLSStateService extends StateService {
    public RCLSStateService(PublisherService publisherService) {
        super(publisherService);
    }

    @Override
    public void onManagerUpdate(PublisherFormat format) {
        executorService.submit(() -> {
            publisherService.dispatchChange(
                    PublisherService.Source.RCLS_STATE_SERVICE,
                    format
            );
        });
    }
}
