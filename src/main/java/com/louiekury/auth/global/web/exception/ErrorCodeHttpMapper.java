package com.louiekury.auth.global.web.exception;

import com.louiekury.auth.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ErrorCodeHttpMapper {

    private ErrorCodeHttpMapper() {
    }

    public static HttpStatus toStatus(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EMAIL_ALREADY_EXISTS, LOGIN_ID_ALREADY_EXISTS, NICKNAME_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case INVALID_EMAIL_VERIFICATION -> HttpStatus.BAD_REQUEST;
            case ACCOUNT_CONFLICT -> HttpStatus.CONFLICT;
            case SIGNUP_PENDING -> HttpStatus.SERVICE_UNAVAILABLE;
            case INVALID_VERIFICATION_CODE -> HttpStatus.BAD_REQUEST;
            case VERIFICATION_EXPIRED -> HttpStatus.CONFLICT;
            case VERIFICATION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case VERIFICATION_DELIVERY_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
