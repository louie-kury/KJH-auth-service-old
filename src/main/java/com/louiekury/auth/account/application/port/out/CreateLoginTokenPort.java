package com.louiekury.auth.account.application.port.out;

import com.louiekury.auth.account.application.login.CreateLoginResult;

public interface CreateLoginTokenPort {
    CreateLoginResult createLoginToken(String memberId);
}
