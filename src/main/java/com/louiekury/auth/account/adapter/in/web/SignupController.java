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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_EXISTS, LOGIN_ID_ALREADY_EXISTS, NICKNAME_ALREADY_EXISTS, ACCOUNT_CONFLICT", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "VERIFICATION_UNAVAILABLE 또는 SIGNUP_PENDING", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Response>> signup(@RequestBody Request request) {
        if (request == null) invalid("요청 본문은 필수입니다.");
        validate(request.name(), 100, "name");
        validate(request.phoneNumber(), 30, "phoneNumber");
        if (!request.phoneNumber().matches("[0-9]+")) invalid("phoneNumber는 숫자만 입력해야 합니다.");
        validate(request.gender(), 20, "gender");
        if (!request.gender().equals("MALE") && !request.gender().equals("FEMALE")) invalid("gender는 MALE 또는 FEMALE이어야 합니다.");
        if (request.birthDate() == null) invalid("birthDate는 필수입니다.");
        validate(request.address(), 255, "address");
        validate(request.nickname(), 30, "nickname");
        validate(request.loginId(), 100, "loginId");
        validatePassword(request.password());
        validate(request.email(), 254, "email");
        String email = request.email().trim();
        if (email.length() > 254 || email.indexOf('@') > 64 || !EMAIL.matcher(email).matches()) invalid("email은 올바른 ASCII 이메일 형식이어야 합니다.");
        validate(request.emailVerificationToken(), 100, "emailVerificationToken");
        String memberId = useCase.signup(new SignupCommand(request.name(), request.phoneNumber(),
                request.gender(), request.birthDate(), request.address(), request.nickname(),
                request.loginId(), request.password(), request.email(), request.emailVerificationToken()));
        return ResponseEntity.created(URI.create("/api/members/me")).body(ApiResponse.success(new Response(memberId)));
    }

    private static void validate(String value, int max, String field) {
        if (value == null || value.isBlank()) invalid(field + "은(는) 필수입니다.");
        if (value.codePointCount(0, value.length()) > max) invalid(field + "은(는) 최대 " + max + "자입니다.");
    }
    private static void validatePassword(String value) {
        if (value == null) invalid("password는 필수입니다.");
        int length = value.codePointCount(0, value.length());
        if (length < 8 || length > 128) invalid("password는 8~128자여야 합니다.");
    }
    private static void invalid(String message) { throw new BusinessException(ErrorCode.VALIDATION_ERROR, message); }

    @Schema(name = "SignupRequest")
    public record Request(
            @Schema(description = "필수, 최대 100 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(description = "필수, 숫자만, 최대 30 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber,
            @Schema(allowableValues = {"MALE", "FEMALE"}, requiredMode = Schema.RequiredMode.REQUIRED) String gender,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate birthDate,
            @Schema(description = "필수, 최대 255 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String address,
            @Schema(description = "필수, 최대 30 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
            @Schema(description = "필수, 최대 100 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED) String loginId,
            @Schema(description = "필수, 8~128 코드 포인트", example = "<password>", requiredMode = Schema.RequiredMode.REQUIRED) String password,
            @Schema(description = "필수 ASCII 이메일, 최대 254자", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
            @Schema(description = "필수 인증 증명, 최대 100 코드 포인트", example = "<email-verification-token>", requiredMode = Schema.RequiredMode.REQUIRED) String emailVerificationToken) {
        @Override public String toString() { return "SignupRequest[redacted]"; }
    }
    @Schema(name = "SignupResponse")
    public record Response(String memberId) { }
}
