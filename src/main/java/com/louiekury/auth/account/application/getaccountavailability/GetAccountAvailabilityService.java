package com.louiekury.auth.account.application.getaccountavailability;

import com.louiekury.auth.account.application.port.out.GetAccountAvailabilityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetAccountAvailabilityService implements GetAccountAvailabilityUseCase {
    private final GetAccountAvailabilityPort lookup;


    @Override
    public GetAccountAvailabilityResult getLoginIdAvailability(String loginId) {
        return GetAccountAvailabilityResult.of(!lookup.getLoginIdExists(loginId));
    }

    @Override
    public GetAccountAvailabilityResult getNicknameAvailability(String nickname) {
        return GetAccountAvailabilityResult.of(!lookup.getNicknameExists(nickname));
    }
}
