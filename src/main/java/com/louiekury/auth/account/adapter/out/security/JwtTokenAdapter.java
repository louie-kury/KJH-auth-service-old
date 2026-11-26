package com.louiekury.auth.account.adapter.out.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.louiekury.auth.account.application.login.CreateLoginResult;
import com.louiekury.auth.account.application.port.out.CreateLoginTokenPort;
import com.louiekury.auth.account.application.port.out.GetJwksPort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenAdapter implements CreateLoginTokenPort, GetJwksPort {
    private final RSAKey key;
    private final String issuer;
    private final String audience;
    private final List<Map<String, Object>> publicKeys = new ArrayList<>();

    public JwtTokenAdapter(@Value("${auth.jwt.private-key-path}") String keyPath,
                           @Value("${auth.jwt.issuer}") String issuer,
                           @Value("${auth.jwt.audience}") String audience,
                           @Value("${auth.jwt.key-id}") String keyId) {
        if (issuer.isBlank() || audience.isBlank() || keyId.isBlank()) {
            throw new IllegalArgumentException("JWT configuration is required");
        }
        this.issuer = issuer;
        this.audience = audience;
        try {
            String pem = Files.readString(Path.of(keyPath)).replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            var privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            if (privateKey.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("RSA key must be at least 2048 bits");
            }
            var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            this.key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).build();
            publicKeys.add(this.key.toPublicJWK().toJSONObject());
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid JWT private key", ex);
        }
    }

    @Value("${auth.jwt.additional-public-keys:}")
    public void setAdditionalPublicKeys(String entries) {
        if (entries.isBlank()) return;
        try {
            for (String entry : entries.split(",")) {
                String[] pair = entry.strip().split("=", 2);
                if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()
                    || publicKeys.stream().anyMatch(k -> pair[0].equals(k.get("kid")))) {
                    throw new IllegalArgumentException("Invalid additional key configuration");
                }
                String pem = Files.readString(Path.of(pair[1])).replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
                var oldKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
                if (oldKey.getModulus().bitLength() < 2048) throw new IllegalArgumentException("RSA key too short");
                publicKeys.add(new RSAKey.Builder(oldKey).keyID(pair[0]).build().toJSONObject());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid additional JWT public key", ex);
        }
    }

    @Override
    public CreateLoginResult createLoginToken(String memberId) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(900);
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(memberId)
            .issueTime(Date.from(now)).expirationTime(Date.from(expires)).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException ex) {
            throw new IllegalStateException("Token signing failed", ex);
        }
        return CreateLoginResult.of(jwt.serialize(), "Bearer", expires, memberId);
    }

    @Override
    public Map<String, Object> getJwks() { return Map.of("keys", List.copyOf(publicKeys)); }
}
