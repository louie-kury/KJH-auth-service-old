package com.louiekury.auth.global.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration(proxyBeanMethods = false)
@EnableMongoAuditing(dateTimeProviderRef = "mongoDateTimeProvider")
public class MongoAuditingConfig {

    @Bean
    public DateTimeProvider mongoDateTimeProvider(Clock businessClock) {
        return () -> Optional.of(businessClock.instant());
    }
}
