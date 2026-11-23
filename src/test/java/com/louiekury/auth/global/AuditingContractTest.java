package com.louiekury.auth.global;

import com.louiekury.auth.global.config.JpaAuditingConfig;
import com.louiekury.auth.global.persistence.BaseEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mapping.context.PersistentEntities;
import static org.assertj.core.api.Assertions.*;

class AuditingContractTest {
    private static final Instant CREATED = Instant.parse("2026-11-19T15:00:00Z");
    private static final Instant UPDATED = CREATED.plusSeconds(30);

    @Test
    void jpaProviderAndAuditingKeepKoreanWallTimeOnCreateAndUpdate() {
        JpaRecord record = new JpaRecord();
        AuditingHandler handler = handler(new JpaAuditingConfig().jpaDateTimeProvider(clock(CREATED)));
        handler.markCreated(record);
        assertThat(record.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-11-20T00:00:00"));
        assertThat(record.getUpdatedAt()).isEqualTo(record.getCreatedAt());
        handler.setDateTimeProvider(new JpaAuditingConfig().jpaDateTimeProvider(clock(UPDATED)));
        handler.markModified(record);
        assertThat(record.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-11-20T00:00:00"));
        assertThat(record.getUpdatedAt()).isEqualTo(LocalDateTime.parse("2026-11-20T00:00:30"));
        assertThat(record.getDeletedAt()).isNull();
    }

    @Test
    void jpaDeletionRejectsMissingFirstTimeAndPreservesFirstDeletion() {
        JpaRecord record = new JpaRecord();
        assertThatNullPointerException().isThrownBy(() -> record.markDeleted(null));
        assertThat(record.getDeletedAt()).isNull();
        LocalDateTime first = LocalDateTime.parse("2026-11-20T00:00:00");
        record.markDeleted(first);
        record.markDeleted(first.plusDays(1));
        record.markDeleted(null);
        assertThat(record.getDeletedAt()).isEqualTo(first);
    }

    private static Clock clock(Instant instant) { return Clock.fixed(instant, ZoneOffset.UTC); }
    private static AuditingHandler handler(DateTimeProvider provider) {
        AuditingHandler handler = new AuditingHandler(new PersistentEntities(List.of()));
        handler.setDateTimeProvider(provider);
        return handler;
    }
    static class JpaRecord extends BaseEntity { }
}
