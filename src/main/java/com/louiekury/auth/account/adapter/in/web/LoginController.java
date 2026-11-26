package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.dto.request.CreateLoginRequestDto;
import com.louiekury.auth.account.adapter.in.web.dto.response.LoginResponseDto;
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
public class LoginController {
    private final LoginWebAdapter adapter;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> createLogin(@Valid @RequestBody CreateLoginRequestDto request) {
        return adapter.createLogin(request);
    }
}
