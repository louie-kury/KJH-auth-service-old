package com.louiekury.auth.account.application.port.out;

public interface EncodePasswordPort {

    String encode(String rawPassword);
}
