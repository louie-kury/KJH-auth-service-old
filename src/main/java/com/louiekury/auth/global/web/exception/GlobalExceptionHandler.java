package com.louiekury.auth.global.web.exception;

import com.louiekury.auth.global.web.response.OpenApiError;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<OpenApiError> handleBusinessException(BusinessException exception) {
        ErrorCode code = exception.getErrorCode();
        HttpStatus status = ErrorCodeHttpMapper.toStatus(code);
        if (status.is5xxServerError()) {
            log.error("업무 요청 처리 중 서버 오류가 발생했습니다.", exception);
        }
        String message = status.is5xxServerError() || exception.getMessage() == null
            ? code.getMessage() : exception.getMessage();
        return ResponseEntity.status(status).body(errorBody(status, code.name(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<OpenApiError> handleValidation() {
        return build(ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenApiError> handleUnexpectedException(Exception exception) {
        if (exception instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                .body(requestErrorBody(error.getStatusCode()));
        }
        log.error("요청 처리 중 예상하지 못한 예외가 발생했습니다.", exception);
        if (exception instanceof ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                .body(requestErrorBody(error.getStatusCode()));
        }
        return build(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("요청 처리 중 서버 오류가 발생했습니다.", exception);
        }
        return super.handleExceptionInternal(exception, requestErrorBody(status), headers, status, request);
    }

    private OpenApiError requestErrorBody(HttpStatusCode status) {
        ErrorCode code = switch (status.value()) {
            case 400 -> ErrorCode.VALIDATION_ERROR;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_SERVER_ERROR : null;
        };
        if (code != null) return errorBody(status, code.name(), code.getMessage());
        HttpStatus standardStatus = HttpStatus.resolve(status.value());
        return errorBody(status, standardStatus == null ? "HTTP_" + status.value() : standardStatus.name(),
            "요청을 처리할 수 없습니다.");
    }

    private ResponseEntity<OpenApiError> build(ErrorCode code) {
        HttpStatus status = ErrorCodeHttpMapper.toStatus(code);
        return ResponseEntity.status(status).body(errorBody(status, code.name(), code.getMessage()));
    }

    private OpenApiError errorBody(HttpStatusCode status, String code, String detail) {
        HttpStatus standardStatus = HttpStatus.resolve(status.value());
        String title = standardStatus == null ? "HTTP " + status.value() : standardStatus.getReasonPhrase();
        return new OpenApiError(title, status.value(), detail, code);
    }
}