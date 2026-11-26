package com.louiekury.auth.account.application.port.out;

import com.louiekury.auth.account.application.createsignup.CreateSignupCommand;
import java.util.Optional;

public interface CreateSignupPort {
    Optional<AccountConflict> getAccountConflict(CreateSignupCommand command);
    String createSignup(CreateSignupCommand command, String passwordHash, String accountStatus, String userType);

    enum AccountConflict { EMAIL, LOGIN_ID, NICKNAME }

    class DuplicateAccountException extends RuntimeException {
        public DuplicateAccountException(Throwable cause) { super(cause); }
    }

    class StorageException extends RuntimeException {
        public StorageException(Throwable cause) { super(cause); }
    }
}
