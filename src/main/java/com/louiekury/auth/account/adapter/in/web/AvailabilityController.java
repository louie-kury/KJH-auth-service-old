package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.application.availability.AvailabilityUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Operation(summary = "아이디 중복 확인", description = "필수, 공백 불가, 최대 100 코드 포인트. 결과는 값을 예약하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능 여부")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @GetMapping("/login-ids/availability")
    public ApiResponse<Response> loginIdAvailability(@RequestParam String loginId) {
        validate(loginId, 100);
        return ApiResponse.success(new Response(useCase.isLoginIdAvailable(loginId)));
    }

    @Operation(summary = "닉네임 중복 확인", description = "필수, 공백 불가, 최대 30 코드 포인트. 결과는 값을 예약하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능 여부")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
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
