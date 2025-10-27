package com.julianw03.rcls.controller.interceptors.loggedIn;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class UserNotLoggedInException extends ErrorResponseException {
    public UserNotLoggedInException() {
        super(HttpStatus.UNAUTHORIZED);
        setTitle("Not logged in");
        setDetail("Please log in with the riot client to access this resource.");
    }
}
