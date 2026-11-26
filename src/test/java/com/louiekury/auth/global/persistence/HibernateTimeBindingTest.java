package com.louiekury.auth.global.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.TimeZone;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 DB 왕복이 아닌 Hibernate JDBC 바인딩 경계의 회귀 검사. */
class HibernateTimeBindingTest {
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Los_Angeles", "Asia/Seoul"})
    @DisplayName("JVM 시간대와 관계없이 JDBC 경계에서 한국 현지 시각을 유지한다")
    void applicationSettingsPreserveKoreanWallTimeAtJdbcBoundary(String defaultZone) throws Exception {
        var builder = new StandardServiceRegistryBuilder();
        for (var source : new YamlPropertySourceLoader().load("application", new
            ClassPathResource("application.yaml"))) {
            for (String name : ((org.springframework.core.env.EnumerablePropertySource<?>) source).getPropertyNames()) {
                String prefix = "spring.jpa.properties.";
                if (name.startsWith(prefix)) {
                    builder.applySetting(name.substring(prefix.length()), source.getProperty(name));
                }
            }
        }
        // 테스트 환경은 DB가 없으므로 메타데이터 조회와 스키마 생성을 하지 않는다.
        builder.applySettings(Map.of("hibernate.dialect", "org.hibernate.dialect.MySQLDialect",
            "hibernate.boot.allow_jdbc_metadata_access", false, "hibernate.hbm2ddl.auto", "none"));
        TimeZone previous = TimeZone.getDefault();
        try (var registry = builder.build()) {
            TimeZone.setDefault(TimeZone.getTimeZone(defaultZone));
            try (var factory = (SessionFactoryImplementor) new MetadataSources(registry)
                .addAnnotatedClass(BindingRecord.class).buildMetadata().buildSessionFactory()) {
                TypeConfiguration types = factory.getTypeConfiguration();
                JdbcTypeIndicators indicators = new JdbcTypeIndicators() {
                    public TypeConfiguration getTypeConfiguration() { return types; }
                    public org.hibernate.dialect.Dialect getDialect() { return factory.getJdbcServices().getDialect(); }
                    public boolean isPreferJavaTimeJdbcTypesEnabled() {
                        return factory.getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled();
                    }
                };
                var jdbcType = LocalDateTimeJavaType.INSTANCE.getRecommendedJdbcType(indicators);
                AtomicReference<LocalDateTime> bound = new AtomicReference<>();
                PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                        if (method.getName().equals("setObject")) {
                            bound.set((LocalDateTime) args[1]);
                        } else if (method.getName().equals("setTimestamp")) {
                            Timestamp value = (Timestamp) args[1];
                            if (args.length == 3) {
                                Calendar calendar = (Calendar) args[2];

                                    bound.set(value.toInstant()
                                        .atZone(calendar.getTimeZone().toZoneId()).toLocalDateTime());
                            } else {
                                bound.set(value.toLocalDateTime());
                            }
                        } else {
                            throw new UnsupportedOperationException(method.getName());
                        }
                        return null;
                    });
                WrapperOptions options = (WrapperOptions) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WrapperOptions.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getJdbcTimeZone" -> factory.getSessionFactoryOptions().getJdbcTimeZone();
                        case "getTypeConfiguration" -> types;
                        case "getSessionFactory" -> factory;
                        case "getDialect" -> factory.getJdbcServices().getDialect();
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
                LocalDateTime koreanMidnight = LocalDateTime.parse("2026-11-20T00:00:00");
                jdbcType.getBinder(LocalDateTimeJavaType.INSTANCE).bind(statement, koreanMidnight, 1, options);
                assertThat(bound.get()).as("JDBC wall time with JVM zone %s", defaultZone).isEqualTo(koreanMidnight);
            }
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Entity(name = "TimeBindingRecord")
    static class BindingRecord {
        @Id Long id;
        LocalDateTime createdAt;
    }
}
