package com.louiekury.auth.account.application.port.out;

import com.louiekury.auth.account.application.login.GetLoginAccountResult;
import java.util.Optional;

public interface GetLoginAccountPort {
    Optional<GetLoginAccountResult> getLoginAccount(String loginId);
}
