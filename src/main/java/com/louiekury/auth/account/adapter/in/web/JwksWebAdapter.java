package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.application.getjwks.GetJwksUseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwksWebAdapter {
    private final GetJwksUseCase useCase;

    public Map<String, Object> getJwks() {
        return useCase.getJwks();
    }
}
