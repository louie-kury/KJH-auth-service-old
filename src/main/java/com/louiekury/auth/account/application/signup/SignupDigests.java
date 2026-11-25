package com.louiekury.auth.account.application.signup;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Versioned, unambiguous keyed digests; request secrets are never persisted. */
@Component
public class SignupDigests {
    private final byte[] key;

    public SignupDigests(@Value("${signup.idempotency-hmac-key:}") String secret,
                         @Value("${verification.email-hmac-key:}") String emailSecret,
                         @Value("${verification.code-hmac-key:}") String codeSecret) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
        if (!secret.isEmpty() && (secret.equals(emailSecret) || secret.equals(codeSecret))) {
            throw new IllegalStateException("Signup idempotency key must be distinct");
        }
    }

    public byte[] keyDigest(String value) {
        return digest("signup-key-v1", value);
    }

    public byte[] requestDigest(SignupCommand command) {
        return digest("signup-request-v1", command.name(), command.phoneNumber(), command.gender(),
                command.birthDate().toString(), command.address(), command.nickname(), command.loginId(),
                command.password(), command.email(), command.emailVerificationToken());
    }

    public boolean matches(byte[] left, byte[] right) {
        return left != null && right != null && MessageDigest.isEqual(left, right);
    }

    private byte[] digest(String domain, String... values) {
        if (key.length < 32) {
            throw new IllegalStateException("Signup idempotency key is not configured");
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                write(out, domain);
                for (String value : values) write(out, value);
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(buffer.toByteArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot calculate signup digest", e);
        }
    }

    private static void write(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }
}
