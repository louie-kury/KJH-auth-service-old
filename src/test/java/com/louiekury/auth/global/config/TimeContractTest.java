package com.louiekury.auth.global.config;

import com.louiekury.auth.global.config.TimeConfig;
import com.louiekury.auth.global.web.response.ApiTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TimeContractTest {
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Los_Angeles", "Asia/Tokyo"})
    @DisplayName("JVM 기본 시간대와 관계없이 한국 자정 경계를 유지한다")
    void midnightBoundaryIsKoreanTimeRegardlessOfJvmDefault(String zone) {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            LocalDateTime before = LocalDateTime.parse("2026-11-19T23:59:59");
            LocalDateTime midnight = LocalDateTime.parse("2026-11-20T00:00:00");
            assertThat(ApiTime.from(before).toString()).isEqualTo("2026-11-19T23:59:59+09:00");
            assertThat(ApiTime.from(midnight).toString()).isEqualTo("2026-11-20T00:00+09:00");
            assertThat(ApiTime.from(midnight).toInstant()).isEqualTo(Instant.parse("2026-11-19T15:00:00Z"));
            assertThat(ApiTime.from(before).getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    @DisplayName("시계에 지정된 업무 시간대를 사용한다")
    void clockUsesExplicitBusinessZone() {
        assertThat(new TimeConfig().businessClock().getZone().getId()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("시간이 없을 때 임의의 타임스탬프를 만들지 않는다")
    void absentTimeCannotBecomeAnInventedTimestamp() {
        assertThatNullPointerException().isThrownBy(() -> ApiTime.from(null));
    }
}
