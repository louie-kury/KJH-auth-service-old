package com.louiekury.auth.account.application.port.out;

public interface GetAccountAvailabilityPort {
    boolean getLoginIdExists(String loginId);
    boolean getNicknameExists(String nickname);
}