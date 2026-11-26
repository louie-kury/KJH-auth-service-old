package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.dto.response.AvailabilityResponseDto;
import com.louiekury.auth.account.application.getaccountavailability.GetAccountAvailabilityUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityWebAdapter {
    private final GetAccountAvailabilityUseCase useCase;

    public ApiResponse<AvailabilityResponseDto> getLoginIdAvailability(String loginId) {
        validate(loginId, 100);
        return ApiResponse.success(AvailabilityResponseDto.from(useCase.getLoginIdAvailability(loginId)));
    }

    public ApiResponse<AvailabilityResponseDto> getNicknameAvailability(String nickname) {
        validate(nickname, 30);
        return ApiResponse.success(AvailabilityResponseDto.from(useCase.getNicknameAvailability(nickname)));
    }

    private static void validate(String value, int maxLength) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
