package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.docs.AvailabilityApiDocumentation;

import com.louiekury.auth.account.adapter.in.web.dto.response.AvailabilityResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AvailabilityController implements AvailabilityApiDocumentation {
    private final AvailabilityWebAdapter adapter;

    @Override
    @GetMapping("/login-ids/availability")
    public ApiResponse<AvailabilityResponseDto> getLoginIdAvailability(@RequestParam String loginId) {
        return adapter.getLoginIdAvailability(loginId);
    }

    @Override
    @GetMapping("/nicknames/availability")
    public ApiResponse<AvailabilityResponseDto> getNicknameAvailability(@RequestParam String nickname) {
        return adapter.getNicknameAvailability(nickname);
    }
}
