package com.louiekury.auth.global;

import com.louiekury.auth.global.config.TimeConfig;
import com.louiekury.auth.global.web.response.ApiTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class TimeContractTest {
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Los_Angeles", "Asia/Tokyo"})
    void midnightBoundaryIsKoreanTimeRegardlessOfJvmDefault(String zone) {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            Instant before = Instant.parse("2026-11-19T14:59:59Z");
            Instant midnight = Instant.parse("2026-11-19T15:00:00Z");
            assertThat(ApiTime.from(before).toString()).isEqualTo("2026-11-19T23:59:59+09:00");
            assertThat(ApiTime.from(midnight).toString()).isEqualTo("2026-11-20T00:00+09:00");
            assertThat(ApiTime.from(midnight).toInstant()).isEqualTo(midnight);
            assertThat(ApiTime.from(LocalDateTime.parse("2026-11-20T00:00:00")))
                .isEqualTo(ApiTime.from(midnight));
            assertThat(ApiTime.from(before).getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void clockUsesExplicitBusinessZone() {
        assertThat(new TimeConfig().businessClock().getZone().getId()).isEqualTo("Asia/Seoul");
    }

    @Test
    void absentTimesCannotBecomeInventedTimestamps() {
        assertThatNullPointerException().isThrownBy(() -> ApiTime.from((Instant) null));
        assertThatNullPointerException().isThrownBy(() -> ApiTime.from((LocalDateTime) null));
    }
}
