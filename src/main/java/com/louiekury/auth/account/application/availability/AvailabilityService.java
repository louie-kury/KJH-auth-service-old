package com.louiekury.auth.account.application.availability;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService implements AvailabilityUseCase {
    private final AvailabilityLookup lookup;

    public AvailabilityService(AvailabilityLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public boolean isLoginIdAvailable(String loginId) {
        return !lookup.loginIdExists(loginId);
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !lookup.nicknameExists(nickname);
    }
}
