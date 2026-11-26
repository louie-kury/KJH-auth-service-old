package com.louiekury.auth.emailverification.application.createemailverification;

import com.louiekury.auth.emailverification.application.port.out.CreateVerificationDeliveryPort;
import com.louiekury.auth.emailverification.application.port.out.CreateVerificationPort;
import com.louiekury.auth.emailverification.application.port.out.GetRegisteredEmailPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CreateEmailVerificationService implements CreateEmailVerificationUseCase {
    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
            + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private final GetRegisteredEmailPort lookup;
    private final CreateVerificationPort store;
    private final CreateVerificationDeliveryPort sender;
    private final SecureRandom random = new SecureRandom();

    public SendVerificationResult createEmailVerification(String email, String purpose, String clientIp) {
        if (email == null || !"SIGNUP".equals(purpose) || clientIp == null || clientIp.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String trimmed = email.trim();
        if (trimmed.length() > 254 || trimmed.indexOf('@') > 64 || !EMAIL.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (lookup.getRegisteredEmailExists(normalized)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String id = "verify_" + UUID.randomUUID();
        var expiresAt = store.createVerification(normalized, code, id, clientIp);
        sender.createVerificationDelivery(trimmed, code);
        return SendVerificationResult.of(id, expiresAt);
    }
}
