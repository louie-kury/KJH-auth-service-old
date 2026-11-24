package com.louiekury.auth.account.application.availability;

public interface AvailabilityUseCase {
    boolean isLoginIdAvailable(String loginId);
    boolean isNicknameAvailable(String nickname);
}
