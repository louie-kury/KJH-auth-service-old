package com.louiekury.auth.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "jpaDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider jpaDateTimeProvider(Clock businessClock) {
        return () -> Optional.of(LocalDateTime.ofInstant(businessClock.instant(), TimeConfig.BUSINESS_ZONE));
    }
}
