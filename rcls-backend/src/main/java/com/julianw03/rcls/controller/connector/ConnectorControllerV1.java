package com.julianw03.rcls.controller.connector;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/connector/v1")
public class ConnectorControllerV1 {

    private final RiotClientService riotClientService;

    @Autowired
    public ConnectorControllerV1(
            RiotClientService riotClientService
    ) {
        this.riotClientService = riotClientService;
    }

    @PostMapping(value = "/connect", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiotClientConnectionParameters> getConnectToRiotClientService() {
        riotClientService.connect();

        RiotClientConnectionParameters parameters = riotClientService.getConnectionParameters();
        return new ResponseEntity<>(parameters, HttpStatus.OK);
    }

    @GetMapping(value = "/parameters")
    public ResponseEntity<RiotClientConnectionParameters> getCurrentParameters() {
        RiotClientConnectionParameters params = riotClientService.getConnectionParameters();
        if (params == null) {
            throw new APIException("Failed to get Process Parameters", "Maybe the application is not connected ?", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(params, HttpStatus.OK);
    }
}
