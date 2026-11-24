package com.louiekury.auth.account.application.availability;

public interface AvailabilityLookup {
    boolean loginIdExists(String loginId);
    boolean nicknameExists(String nickname);
}
