package com.louiekury.auth.account.application.port.out;

public interface CreatePasswordHashPort {

    String createPasswordHash(String rawPassword);
}
