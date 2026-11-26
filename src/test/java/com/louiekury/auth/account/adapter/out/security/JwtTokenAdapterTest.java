package com.louiekury.auth.account.adapter.out.security;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.*;

class JwtTokenAdapterTest {
    @TempDir Path dir;

    @Test
    @DisplayName("공개키로 서명과 토큰 정보를 검증하고 유효기간은 15분이다")
    void jwksVerifiesSignedTokenAndClaimsHaveFifteenMinuteLifetime() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        Path pem = dir.resolve("key.pem");
        Files.writeString(pem, "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n");
        var service = new JwtTokenAdapter(pem.toString(), "test-issuer", "test-audience", "test-key");
        var result = service.createLoginToken("member-id");
        var jwks = service.getJwks();
        var keys = (List<?>) jwks.get("keys");
        var publicKey = RSAKey.parse((Map<String, Object>) keys.get(0));
        assertThat(publicKey.isPrivate()).isFalse();
        assertThat(publicKey.getKeyID()).isEqualTo("test-key");
        var jwt = SignedJWT.parse(result.getAccessToken());
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(jwt.verify(new RSASSAVerifier(publicKey))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("member-id");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("test-issuer");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("test-audience");
        var issued = jwt.getJWTClaimsSet().getIssueTime().toInstant();
        var expiry = jwt.getJWTClaimsSet().getExpirationTime().toInstant();
        assertThat(expiry).isEqualTo(issued.plusSeconds(900)).isAfter(Instant.now());
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getMemberId()).isEqualTo("member-id");
        assertThat(result.getExpiresAt().getEpochSecond()).isEqualTo(expiry.getEpochSecond());
        String[] parts = result.getAccessToken().split("\\.");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"attacker\"}".getBytes());
        assertThat(SignedJWT.parse(String.join(".", parts)).verify(new RSASSAVerifier(publicKey))).isFalse();

        var verifier = new com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier<>("test-audience",
            new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("test-issuer").build(),
            java.util.Set.of("sub", "exp", "iat"));
        verifier.setMaxClockSkew(0);
        verifier.verify(jwt.getJWTClaimsSet(), null);
        var expired = new SignedJWT(jwt.getHeader(), new com.nimbusds.jwt.JWTClaimsSet.Builder(jwt.getJWTClaimsSet())
            .expirationTime(java.util.Date.from(Instant.now().minusSeconds(60))).build());
        expired.sign(new com.nimbusds.jose.crypto.RSASSASigner((java.security.interfaces.RSAPrivateKey) pair.getPrivate()));
        assertThat(expired.verify(new RSASSAVerifier(publicKey))).isTrue();
        assertThatThrownBy(() -> verifier.verify(expired.getJWTClaimsSet(), null))
            .isInstanceOf(com.nimbusds.jwt.proc.BadJWTException.class);
    }

    @Test
    @DisplayName("개인키가 없거나 잘못되면 초기화가 실패한다")
    void missingOrInvalidPrivateKeyFailsStartup() throws Exception {
        assertThatThrownBy(() -> new JwtTokenAdapter(dir.resolve("missing").toString(), "iss", "aud", "key"))
            .isInstanceOf(IllegalStateException.class);
        Path invalid = dir.resolve("invalid.pem");
        Files.writeString(invalid, "not a key");
        assertThatThrownBy(() -> new JwtTokenAdapter(invalid.toString(), "iss", "aud", "key"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("추가 공개키를 게시하고 중복 키 식별자를 거부한다")
    void jwksIncludesAdditionalPublicKeyAndRejectsDuplicateKeyId() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var current = generator.generateKeyPair();
        var previous = generator.generateKeyPair();
        Path privatePem = dir.resolve("current.pem");
        Files.writeString(privatePem, "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(current.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n");
        Path publicPem = dir.resolve("previous.pem");
        Files.writeString(publicPem, "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(previous.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n");
        var service = new JwtTokenAdapter(privatePem.toString(), "iss", "aud", "current");
        service.setAdditionalPublicKeys("previous=" + publicPem);
        var keys = (List<?>) service.getJwks().get("keys");
        assertThat(keys).hasSize(2);
        var oldKey = RSAKey.parse((Map<String, Object>) keys.get(1));
        assertThat(oldKey.getKeyID()).isEqualTo("previous");
        assertThat(oldKey.toRSAPublicKey()).isEqualTo(previous.getPublic());
        assertThat(oldKey.isPrivate()).isFalse();
        assertThatThrownBy(() -> service.setAdditionalPublicKeys("current=" + publicPem))
            .isInstanceOf(IllegalStateException.class);
    }
}
