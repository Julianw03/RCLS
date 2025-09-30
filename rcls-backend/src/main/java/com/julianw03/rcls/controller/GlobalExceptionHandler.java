package com.julianw03.rcls.controller;

import com.julianw03.rcls.model.APIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
public class GlobalExceptionHandler {

//    @ExceptionHandler(APIException.class)
//    public ResponseEntity<ProblemDetail> handleApiException(APIException ex) {
//        ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatus());
//        problemDetail.setDetail(ex.getMessage());
//        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ProblemDetail> handleException(Exception ex) {
//        return handleApiException(new APIException(ex));
//    }
}
