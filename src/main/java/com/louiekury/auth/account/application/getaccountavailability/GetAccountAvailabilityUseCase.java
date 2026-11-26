package com.louiekury.auth.account.application.getaccountavailability;

public interface GetAccountAvailabilityUseCase {
    GetAccountAvailabilityResult getLoginIdAvailability(String loginId);
    GetAccountAvailabilityResult getNicknameAvailability(String nickname);
}
