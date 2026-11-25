package com.louiekury.auth.account.application.signup;

public interface SignupUseCase {
    String signup(String key, SignupCommand command);
}
