package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.dto.request.SignupRequestDto;
import com.louiekury.auth.account.adapter.in.web.dto.response.SignupResponseDto;
import com.louiekury.auth.account.application.createsignup.CreateSignupCommand;
import com.louiekury.auth.account.application.createsignup.CreateSignupUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupWebAdapter {
    private final CreateSignupUseCase useCase;

    public ResponseEntity<ApiResponse<SignupResponseDto>> createSignup(SignupRequestDto request) {
        var result = useCase.createSignup(CreateSignupCommand.of(request.getName(), request.getPhoneNumber(),
                request.getGender(), request.getBirthDate(), request.getAddress(), request.getNickname(),
                request.getLoginId(), request.getPassword(), request.getEmail(), request.getEmailVerificationToken()));
        return ResponseEntity.created(URI.create("/api/members/me"))
            .body(ApiResponse.success(SignupResponseDto.from(result)));
    }
}
