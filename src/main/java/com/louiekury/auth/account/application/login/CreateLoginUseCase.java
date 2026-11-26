package com.louiekury.auth.account.application.login;

public interface CreateLoginUseCase {
    CreateLoginResult createLogin(CreateLoginCommand command);
}
