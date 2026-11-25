package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.out.persistence.SignupCleanupScheduler;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SignupCleanupSchedulingTest {
    @Test
    void scheduledCleanupRetriesAfterDatabaseFailure() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString())).thenThrow(new DataAccessResourceFailureException("synthetic failure")).thenReturn(1);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "signup.cleanup-initial-delay-ms", "20", "signup.cleanup-interval-ms", "50")));
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.register(SignupCleanupScheduler.class);
            context.refresh();
            assertThat(context.getBean(SignupCleanupScheduler.class)).isNotNull();
            verify(jdbc, timeout(4000).atLeast(2)).update(contains("DELETE FROM signup_idempotency"));
        }
    }
}
