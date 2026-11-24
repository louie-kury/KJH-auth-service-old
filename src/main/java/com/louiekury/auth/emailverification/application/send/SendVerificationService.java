package com.louiekury.auth.emailverification.application.send;

import com.louiekury.auth.emailverification.application.port.out.RegisteredEmailLookup;
import com.louiekury.auth.emailverification.application.port.out.VerificationSender;
import com.louiekury.auth.emailverification.application.port.out.VerificationStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SendVerificationService implements SendVerificationUseCase {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private final RegisteredEmailLookup lookup;
    private final VerificationStore store;
    private final VerificationSender sender;
    private final SecureRandom random = new SecureRandom();

    public SendVerificationService(RegisteredEmailLookup lookup, VerificationStore store, VerificationSender sender) {
        this.lookup = lookup;
        this.store = store;
        this.sender = sender;
    }

    public Result send(String email, String purpose, String clientIp) {
        if (email == null || !"SIGNUP".equals(purpose) || clientIp == null || clientIp.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String trimmed = email.trim();
        if (trimmed.length() > 254 || trimmed.indexOf('@') > 64 || !EMAIL.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (lookup.exists(normalized)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String id = "verify_" + UUID.randomUUID();
        var expiresAt = store.reserve(normalized, code, id, clientIp);
        sender.send(trimmed, code);
        return new Result(id, expiresAt);
    }
}
