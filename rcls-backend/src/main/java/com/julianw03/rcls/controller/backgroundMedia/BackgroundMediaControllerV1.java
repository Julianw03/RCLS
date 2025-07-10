package com.julianw03.rcls.controller.backgroundMedia;

import com.julianw03.rcls.service.rest.BackgroundMediaV1RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/background-media/v1")
public class BackgroundMediaControllerV1 {
    private final BackgroundMediaV1RestService restService;

    public BackgroundMediaControllerV1(
            @Autowired BackgroundMediaV1RestService restService
    ) {
        this.restService = restService;
    }


}
