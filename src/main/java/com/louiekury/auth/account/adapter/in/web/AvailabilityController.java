package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.application.availability.AvailabilityUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AvailabilityController {
    private final AvailabilityUseCase useCase;

    public AvailabilityController(AvailabilityUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/login-ids/availability")
    public ApiResponse<Response> loginIdAvailability(@RequestParam String loginId) {
        validate(loginId, 100);
        return ApiResponse.success(new Response(useCase.isLoginIdAvailable(loginId)));
    }

    @GetMapping("/nicknames/availability")
    public ApiResponse<Response> nicknameAvailability(@RequestParam String nickname) {
        validate(nickname, 30);
        return ApiResponse.success(new Response(useCase.isNicknameAvailable(nickname)));
    }

    private static void validate(String value, int maxLength) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    public record Response(boolean available) {}
}
