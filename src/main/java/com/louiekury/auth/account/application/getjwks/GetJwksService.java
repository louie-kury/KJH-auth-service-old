package com.louiekury.auth.account.application.getjwks;

import com.louiekury.auth.account.application.port.out.GetJwksPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetJwksService implements GetJwksUseCase {
    private final GetJwksPort jwks;

    @Override
    public Map<String, Object> getJwks() {
        return jwks.getJwks();
    }
}
