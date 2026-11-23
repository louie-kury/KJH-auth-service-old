package com.louiekury.auth.global.web.exception;

import com.louiekury.auth.global.web.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode code = exception.getErrorCode();
        HttpStatus status = ErrorCodeHttpMapper.toStatus(code);
        if (status.is5xxServerError()) {
            log.error("업무 요청 처리 중 서버 오류가 발생했습니다.", exception);
        }
        String exceptionMessage = exception.getMessage();
        String message = status.is5xxServerError() || exceptionMessage == null
            ? code.getMessage() : exceptionMessage;
        return ResponseEntity.status(status)
            .body(ApiResponse.error(code.name(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation() {
        return build(ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        if (exception instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            return handleRequestError(error);
        }
        log.error("요청 처리 중 예상하지 못한 예외가 발생했습니다.", exception);
        if (exception instanceof ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode())
                .headers(error.getHeaders())
                .body(errorBody(ErrorCode.INTERNAL_SERVER_ERROR));
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

    private ResponseEntity<ApiResponse<Void>> handleRequestError(ErrorResponse error) {
        return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
            .body(requestErrorBody(error.getStatusCode()));
    }

    private ApiResponse<Void> requestErrorBody(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return errorBody(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return switch (status.value()) {
            case 400 -> errorBody(ErrorCode.VALIDATION_ERROR);
            case 404 -> errorBody(ErrorCode.NOT_FOUND);
            case 405 -> errorBody(ErrorCode.METHOD_NOT_ALLOWED);
            default -> status instanceof HttpStatus standardStatus
                ? ApiResponse.error(standardStatus.name(), standardStatus.getReasonPhrase())
                : ApiResponse.error("HTTP_" + status.value(), "요청을 처리할 수 없습니다.");
        };
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code) {
        return ResponseEntity.status(ErrorCodeHttpMapper.toStatus(code)).body(errorBody(code));
    }

    private ApiResponse<Void> errorBody(ErrorCode code) {
        return ApiResponse.error(code.name(), code.getMessage());
    }
}
