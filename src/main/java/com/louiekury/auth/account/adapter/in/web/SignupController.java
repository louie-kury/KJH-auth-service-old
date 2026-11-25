package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.application.signup.SignupCommand;
import com.louiekury.auth.account.application.signup.SignupUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.net.URI;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SignupController {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private final SignupUseCase useCase;

    public SignupController(SignupUseCase useCase) { this.useCase = useCase; }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Response>> signup(
            @RequestHeader("Idempotency-Key") String key, @RequestBody Request request) {
        validate(key, 255);
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
        String memberId = useCase.signup(key, new SignupCommand(request.name(), request.phoneNumber(),
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

    public record Request(String name, String phoneNumber, String gender, LocalDate birthDate,
                          String address, String nickname, String loginId, String password,
                          String email, String emailVerificationToken) {
        @Override public String toString() { return "SignupRequest[redacted]"; }
    }
    public record Response(String memberId) { }
}
