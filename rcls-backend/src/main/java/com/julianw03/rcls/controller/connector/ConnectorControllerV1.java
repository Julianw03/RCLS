package com.julianw03.rcls.controller.connector;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.base.riotclient.RiotClientService;
import com.julianw03.rcls.service.rest.ConnectorV1RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ConnectorV1RestService connectorV1RestService;

    @Autowired
    public ConnectorControllerV1(
            ConnectorV1RestService riotClientService
    ) {
        this.connectorV1RestService = riotClientService;
    }

    @PostMapping(value = "/connect", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiotClientConnectionParameters> getConnectToRiotClientService() {
        return ResponseEntity.ofNullable(connectorV1RestService.connectToRiotClient());
    }

    @GetMapping(value = "/parameters", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiotClientConnectionParameters> getCurrentParameters() {
        return ResponseEntity.ofNullable(connectorV1RestService.getConnectionParameters());
    }
}
