package com.julianw03.rcls.controller;

import com.julianw03.rcls.controller.errors.ApiProblem;
import com.julianw03.rcls.controller.interceptors.loggedIn.UserNotLoggedInException;
import com.julianw03.rcls.controller.interceptors.serviceReady.ServiceNotReadyException;
import com.julianw03.rcls.service.process.NoSuchProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceNotReadyException.class)
    public ResponseEntity<ApiProblem> handleServiceNotReadyException(ServiceNotReadyException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                             .body(ApiProblem.fromErrorResponseException(ex));
    }

    @ExceptionHandler(UserNotLoggedInException.class)
    public ResponseEntity<ApiProblem> handleUserNotLoggedInException(UserNotLoggedInException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                             .body(ApiProblem.fromErrorResponseException(ex));
    }

    @ExceptionHandler(FailFastException.class)
    public ResponseEntity<ApiProblem> handleFailFastException(FailFastException ex) {

        return ResponseEntity.status(ex.getStatusCode())
                             .body(ApiProblem.fromErrorResponseException(ex));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiProblem> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                             .body(ApiProblem.fromErrorResponseException(ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiProblem> handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        return ResponseEntity.status(pd.getStatus())
                             .body(ApiProblem.fromProblemDetail(pd));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiProblem> handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        return ResponseEntity.status(pd.getStatus())
                             .body(ApiProblem.fromProblemDetail(pd));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleException(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );

        return ResponseEntity.status(pd.getStatus())
                             .body(ApiProblem.fromProblemDetail(pd));
    }

    @ExceptionHandler(NoSuchProcessException.class)
    public ResponseEntity<ApiProblem> handleNoSuchProcessException(NoSuchProcessException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        return ResponseEntity.status(pd.getStatus())
                             .body(ApiProblem.fromProblemDetail(pd));
    }
}
