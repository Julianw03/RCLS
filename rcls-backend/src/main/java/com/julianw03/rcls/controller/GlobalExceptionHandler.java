package com.julianw03.rcls.controller;

import com.julianw03.rcls.model.APIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIException> handleApiException(APIException ex) {
        return new ResponseEntity<>(ex, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIException> handleException(Exception ex) {
        return handleApiException(new APIException(ex));
    }
}
