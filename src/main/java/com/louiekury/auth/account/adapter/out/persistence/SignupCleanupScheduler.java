package com.louiekury.auth.account.adapter.out.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Bounded, database-clock-based maintenance. InnoDB row locks serialize concurrent instances. */
@Configuration
@EnableScheduling
public class SignupCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(SignupCleanupScheduler.class);
    private final JdbcTemplate jdbc;

    public SignupCleanupScheduler(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${signup.cleanup-interval-ms:900000}",
               initialDelayString = "${signup.cleanup-initial-delay-ms:60000}")
    public void cleanupExpired() {
        try {
            jdbc.update("DELETE FROM signup_idempotency WHERE expires_at <= UTC_TIMESTAMP(6) ORDER BY expires_at LIMIT 500");
        } catch (DataAccessException e) {
            // No values, SQL parameters, or exception details are logged.
            log.warn("가입 요청 만료 기록 정리에 실패했습니다. 다음 주기에 재시도합니다.");
        }
    }
}
