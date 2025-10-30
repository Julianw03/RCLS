package com.julianw03.rcls.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class InvalidCredentialsException extends ErrorResponseException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED);
        setTitle("Invalid Credentials");
        setDetail("The credentials are not valid");
    }
}
