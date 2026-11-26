package com.louiekury.auth.account.application.getaccountavailability;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class GetAccountAvailabilityResult {
    private final boolean available;

    public static GetAccountAvailabilityResult of(boolean available) {
        return GetAccountAvailabilityResult.builder().available(available).build();
    }
}