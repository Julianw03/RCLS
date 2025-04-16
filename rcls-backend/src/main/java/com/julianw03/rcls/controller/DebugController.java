package com.julianw03.rcls.controller;

import com.fasterxml.jackson.databind.node.TextNode;
import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.model.RiotClientConnectionParameters;
import com.julianw03.rcls.service.publisher.PublisherService;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/debug")
public class DebugController {
    private final RiotClientService            riotClientService;
    private final PublisherService             publisherService;
    private final RequestMappingHandlerMapping handlerMapping;

    @Autowired
    public DebugController(
            RiotClientService riotClientService,
            PublisherService publisherService,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping
    ) {
        this.riotClientService = riotClientService;
        this.publisherService = publisherService;
        this.handlerMapping = handlerMapping;
    }

    @GetMapping("/mappings")
    public String getControllerMappings() {
        final String path = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        publisherService.dispatchResourceCreated(path, new TextNode("Test!"));
        return path;
    }

    @GetMapping("/parameters")
    public ResponseEntity<RiotClientConnectionParameters> getProcessData() {
        RiotClientConnectionParameters params = riotClientService.getConnectionParameters();
        if (params == null) {
            throw new APIException("Failed to get Process Parameters", "Maybe the application is not connected ?", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(params, HttpStatus.OK);
    }

    @GetMapping("/connect")
    public ResponseEntity<RiotClientConnectionParameters> getDebugData() {
        riotClientService.connect();

        RiotClientConnectionParameters parameters = riotClientService.getConnectionParameters();
        return new ResponseEntity<>(parameters, HttpStatus.OK);
    }
}
