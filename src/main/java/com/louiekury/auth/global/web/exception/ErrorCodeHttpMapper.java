package com.louiekury.auth.global.web.exception;

import com.louiekury.auth.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ErrorCodeHttpMapper {

    private ErrorCodeHttpMapper() {
    }

    public static HttpStatus toStatus(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case EMAIL_ALREADY_REGISTERED -> HttpStatus.CONFLICT;
            case INVALID_VERIFICATION_CODE -> HttpStatus.BAD_REQUEST;
            case VERIFICATION_EXPIRED -> HttpStatus.CONFLICT;
            case VERIFICATION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case VERIFICATION_DELIVERY_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
