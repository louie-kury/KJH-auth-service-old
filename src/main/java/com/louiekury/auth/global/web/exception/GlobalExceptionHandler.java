package com.louiekury.auth.global.web.exception;

import com.louiekury.auth.account.adapter.in.web.dto.request.SignupRequestDto;
import com.louiekury.auth.global.web.response.OpenApiError;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        return build(WebErrorCode.VALIDATION_ERROR);
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
        return build(WebErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("요청 처리 중 서버 오류가 발생했습니다.", exception);
        }
        if (exception instanceof MethodArgumentNotValidException invalid
                && invalid.getBindingResult().getTarget() instanceof SignupRequestDto) {
            var signupError = invalid.getBindingResult().getFieldErrors().stream()
                .min(Comparator.comparingInt(GlobalExceptionHandler::signupFieldOrder)
                    .thenComparingInt(GlobalExceptionHandler::signupConstraintOrder));
            if (signupError.isPresent()) {
                return super.handleExceptionInternal(
                    exception,
                    errorBody(status, WebErrorCode.VALIDATION_ERROR.name(), signupError.get().getDefaultMessage()),
                    headers,
                    status,
                    request
                );
            }
        }
        return super.handleExceptionInternal(exception, requestErrorBody(status), headers, status, request);
    }

    private static int signupFieldOrder(FieldError error) {
        return List.of("name", "phoneNumber", "gender", "birthDate", "address", "nickname", "loginId",
            "password", "email", "emailVerificationToken").indexOf(error.getField());
    }

    private static int signupConstraintOrder(FieldError error) {
        String code = error.getCode();
        if ("NotNull".equals(code) || "NotBlank".equals(code)) {
            return 0;
        }
        if ("CodePointLength".equals(code)) {
            return 1;
        }
        return 2;
    }

    private OpenApiError requestErrorBody(HttpStatusCode status) {
        WebErrorCode code = switch (status.value()) {
            case 400 -> WebErrorCode.VALIDATION_ERROR;
            case 404 -> WebErrorCode.NOT_FOUND;
            case 405 -> WebErrorCode.METHOD_NOT_ALLOWED;
            default -> status.is5xxServerError() ? WebErrorCode.INTERNAL_SERVER_ERROR : null;
        };
        if (code != null) {
            return errorBody(status, code.name(), code.getMessage());
        }
        HttpStatus standardStatus = HttpStatus.resolve(status.value());
        return errorBody(status, standardStatus == null ? "HTTP_" + status.value() : standardStatus.name(),
            "요청을 처리할 수 없습니다.");
    }

    private ResponseEntity<OpenApiError> build(WebErrorCode code) {
        HttpStatus status = code.getStatus();
        return ResponseEntity.status(status).body(errorBody(status, code.name(), code.getMessage()));
    }

    private OpenApiError errorBody(HttpStatusCode status, String code, String detail) {
        HttpStatus standardStatus = HttpStatus.resolve(status.value());
        String title = standardStatus == null ? "HTTP " + status.value() : standardStatus.getReasonPhrase();
        return OpenApiError.of(title, status.value(), detail, code);
    }
}