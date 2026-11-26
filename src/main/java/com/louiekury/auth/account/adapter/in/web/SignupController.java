package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.docs.SignupApiDocumentation;

import com.louiekury.auth.account.adapter.in.web.dto.request.SignupRequestDto;
import com.louiekury.auth.account.adapter.in.web.dto.response.SignupResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SignupController implements SignupApiDocumentation {
    private final SignupWebAdapter adapter;

    @Override
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> createSignup(@Valid @RequestBody SignupRequestDto request) {
        return adapter.createSignup(request);
    }
}
