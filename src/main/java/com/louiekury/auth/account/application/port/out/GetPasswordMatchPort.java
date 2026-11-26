package com.louiekury.auth.account.application.port.out;

public interface GetPasswordMatchPort {
    boolean getPasswordMatch(String password, String hash);
}
