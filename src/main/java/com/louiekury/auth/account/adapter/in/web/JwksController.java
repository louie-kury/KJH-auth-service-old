package com.louiekury.auth.account.adapter.in.web;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {
    private final JwksWebAdapter adapter;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        return adapter.getJwks();
    }
}
