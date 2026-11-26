package com.louiekury.auth.account.application.login;

import com.louiekury.auth.account.application.port.out.CreateLoginTokenPort;
import com.louiekury.auth.account.application.port.out.GetLoginAccountPort;
import com.louiekury.auth.account.application.port.out.GetPasswordMatchPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateLoginService implements CreateLoginUseCase {
    private final GetLoginAccountPort accounts;
    private final GetPasswordMatchPort passwords;
    private final CreateLoginTokenPort tokens;

    @Override
    public CreateLoginResult createLogin(CreateLoginCommand command) {
        var account = accounts.getLoginAccount(command.getLoginId());
        String hash = account.map(GetLoginAccountResult::getPasswordHash).orElse(null);
        boolean valid = passwords.getPasswordMatch(command.getPassword(), hash);
        if (!valid || account.isEmpty() || hash == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!account.get().isAvailable()) {
            throw new BusinessException(ErrorCode.ACCOUNT_UNAVAILABLE);
        }
        return tokens.createLoginToken(account.get().getMemberId());
    }
}
