package com.louiekury.auth.global.web.response;

import com.louiekury.auth.global.config.TimeConfig;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/** API DTO 팩토리에서 감사 시각을 한국시간의 오프셋 포함 값으로 변환한다. */
public final class ApiTime {

    private ApiTime() {
    }

    public static OffsetDateTime from(LocalDateTime koreanDateTime) {
        return Objects.requireNonNull(koreanDateTime, "koreanDateTime")
            .atZone(TimeConfig.BUSINESS_ZONE).toOffsetDateTime();
    }
}
