package com.louiekury.auth.account.application.port.out;

import com.louiekury.auth.account.application.createaccount.CreateAccountData;

public interface CreateAccountPort {
    String createAccount(CreateAccountData account);
}
