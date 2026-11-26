package com.louiekury.auth.account.adapter.in.web.dto.response;

import com.louiekury.auth.account.application.getaccountavailability.GetAccountAvailabilityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@Schema(name = "Response")
public class AvailabilityResponseDto {
    private final boolean available;

    private AvailabilityResponseDto(boolean available) {
        this.available = available;
    }

    public static AvailabilityResponseDto from(GetAccountAvailabilityResult result) {
        return AvailabilityResponseDto.builder().available(result.isAvailable()).build();
    }
}
