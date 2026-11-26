package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.dto.request.CreateLoginRequestDto;
import com.louiekury.auth.account.adapter.in.web.dto.response.LoginResponseDto;
import com.louiekury.auth.account.application.login.CreateLoginCommand;
import com.louiekury.auth.account.application.login.CreateLoginUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginWebAdapter {
    private final CreateLoginUseCase useCase;

    public ResponseEntity<ApiResponse<LoginResponseDto>> createLogin(CreateLoginRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(LoginResponseDto.from(
            useCase.createLogin(CreateLoginCommand.of(request.getLoginId(), request.getPassword())))));
    }
}
