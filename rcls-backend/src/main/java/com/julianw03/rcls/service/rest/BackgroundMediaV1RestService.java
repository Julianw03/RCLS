package com.julianw03.rcls.service.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianw03.rcls.providers.paths.PathProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BackgroundMediaV1RestService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PathProvider pathProvider;

    public BackgroundMediaV1RestService(
            @Autowired PathProvider pathProvider
    ) {
        this.pathProvider = pathProvider;
    }
}
