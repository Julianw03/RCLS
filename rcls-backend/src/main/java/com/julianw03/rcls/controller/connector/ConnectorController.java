package com.julianw03.rcls.controller.connector;

import com.julianw03.rcls.model.APIException;
import com.julianw03.rcls.service.modules.rclient.connector.ConnectorV1Service;
import com.julianw03.rcls.service.modules.rclient.connector.RiotClientConnectionParametersDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/connector/v1")
public class ConnectorController {

    private final ConnectorV1Service connectorV1Service;

    @Autowired
    public ConnectorController(
            ConnectorV1Service riotClientService
    ) {
        this.connectorV1Service = riotClientService;
    }

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully connected to the Riot Client"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict: An attempt to connect to the Riot Client has already been made and is in progress or the connection has already been established.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIException.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error: Unable to connect to the Riot Client due to an unexpected error.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIException.class)
                    )
            )
    })
    @PostMapping(value = "/connect", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiotClientConnectionParametersDTO> getConnectToRiotClientService() {
        final RiotClientConnectionParametersDTO parametersDTO;
        try {
            parametersDTO = connectorV1Service.connect();
        } catch (ExecutionException e) {
            throw new APIException(e);
        } catch (IllegalStateException e) {
            throw new APIException("Unable to connect to the Riot Client", HttpStatus.CONFLICT, "An attempt to connect to the Riot Client has already been made and is in progress or the connection has already been established.");
        }

        return new ResponseEntity<>(parametersDTO, HttpStatus.OK);
    }

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved connection parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Not Found: Unable to retrieve connection parameters, possibly because the Riot Client is not connected.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIException.class)
                    )
            )
    })
    @GetMapping(value = "/parameters", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiotClientConnectionParametersDTO> getCurrentParameters() {
        final RiotClientConnectionParametersDTO parametersDTO;
        try {
            parametersDTO = connectorV1Service.getConnectionParameters();
        } catch (IllegalStateException e) {
            throw new APIException("Unable to retrieve connection parameters", HttpStatus.NOT_FOUND, e.getMessage());
        }

        return new ResponseEntity<>(parametersDTO, HttpStatus.OK);
    }

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Successfully disconnected from the Riot Client"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict: Unable to disconnect from the Riot Client because it is not connected or the disconnection process is already in progress.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIException.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error: Failed to disconnect from the Riot Client due to an unexpected error.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIException.class)
                    )
            )
    })
    @PostMapping(value = "/disconnect")
    public ResponseEntity<Void> disconnectFromRiotClientService() {
        try {
            connectorV1Service.disconnect();
        } catch (IllegalStateException e) {
            throw new APIException("Unable to disconnect from the Riot Client", HttpStatus.CONFLICT, "The Riot Client is not connected or the disconnection process is already in progress.");
        } catch (ExecutionException e) {
            throw new APIException("Failed to disconnect from Riot Client", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }
}
