package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.application.signup.SignupCommand;
import com.louiekury.auth.account.application.signup.SignupUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.net.URI;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SignupController {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private final SignupUseCase useCase;

    public SignupController(SignupUseCase useCase) { this.useCase = useCase; }

    @Operation(summary = "일반 회원가입", description = "이메일 인증 증명은 한 번만 사용할 수 있습니다. 저장 실패 시 다시 인증해야 합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입 성공. Location: /api/members/me")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR 또는 INVALID_EMAIL_VERIFICATION", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED, LOGIN_ID_ALREADY_EXISTS, NICKNAME_ALREADY_EXISTS, ACCOUNT_CONFLICT", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "VERIFICATION_UNAVAILABLE 또는 SIGNUP_PENDING", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Response>> signup(@RequestBody Request request) {
        validate(request.name(), 100);
        validate(request.phoneNumber(), 30);
        if (!request.phoneNumber().matches("[0-9]+")) invalid();
        validate(request.gender(), 20);
        if (!request.gender().equals("MALE") && !request.gender().equals("FEMALE")) invalid();
        if (request.birthDate() == null) invalid();
        validate(request.address(), 255);
        validate(request.nickname(), 30);
        validate(request.loginId(), 100);
        validatePassword(request.password());
        validate(request.email(), 254);
        String email = request.email().trim();
        if (email.length() > 254 || email.indexOf('@') > 64 || !EMAIL.matcher(email).matches()) invalid();
        validate(request.emailVerificationToken(), 100);
        String memberId = useCase.signup(new SignupCommand(request.name(), request.phoneNumber(),
                request.gender(), request.birthDate(), request.address(), request.nickname(),
                request.loginId(), request.password(), request.email(), request.emailVerificationToken()));
        return ResponseEntity.created(URI.create("/api/members/me")).body(ApiResponse.success(new Response(memberId)));
    }

    private static void validate(String value, int max) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > max) invalid();
    }
    private static void validatePassword(String value) {
        if (value == null) invalid();
        int length = value.codePointCount(0, value.length());
        if (length < 15 || length > 128) invalid();
    }
    private static void invalid() { throw new BusinessException(ErrorCode.VALIDATION_ERROR); }

    public record Request(
            @Schema(description = "필수, 최대 100 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(description = "필수, 숫자만, 최대 30 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber,
            @Schema(allowableValues = {"MALE", "FEMALE"}, requiredMode = Schema.RequiredMode.REQUIRED) String gender,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate birthDate,
            @Schema(description = "필수, 최대 255 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String address,
            @Schema(description = "필수, 최대 30 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
            @Schema(description = "필수, 최대 100 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String loginId,
            @Schema(description = "필수, 15~128 코드 포인트", example = "<password>", requiredMode = Schema.RequiredMode.REQUIRED) String password,
            @Schema(description = "필수 ASCII 이메일, 최대 254자", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
            @Schema(description = "필수 인증 증명, 최대 100 코드 포인트", example = "<email-verification-token>", requiredMode = Schema.RequiredMode.REQUIRED) String emailVerificationToken) {
        @Override public String toString() { return "SignupRequest[redacted]"; }
    }
    public record Response(String memberId) { }
}
