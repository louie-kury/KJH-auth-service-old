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
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
