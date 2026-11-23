package com.louiekury.auth.migration;

import com.mongodb.ConnectionString;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MongoMigrationUrlTest {
    @Test
    void keepsDefaultAuthDatabaseWhenAnUnrelatedOptionValueContainsAuthSource() throws Exception {
        String migrationClasspath = System.getProperty("auth.mongoMigrationClasspath");
        assertThat(migrationClasspath).isNotBlank();
        String[] entries = migrationClasspath.split(Pattern.quote(File.pathSeparator));
        URL[] urls = new URL[entries.length];
        for (int index = 0; index < entries.length; index++) {
            urls[index] = Path.of(entries[index]).toUri().toURL();
        }
        try (var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            Class<?> runner = loader.loadClass("com.louiekury.auth.migration.MongoMigrationMain");
            String uri = (String) runner.getMethod("resolveUrl", String.class, String.class)
                .invoke(null, "mongodb://fixture:example@localhost:27017/?appName=authsource=shadow",
                    "louie_auth_fixture");
            ConnectionString parsed = new ConnectionString(uri);
            assertThat(parsed.getDatabase()).isEqualTo("louie_auth_fixture");
            assertThat(parsed.getApplicationName()).isEqualTo("authsource=shadow");
            assertThat(parsed.getCredential().getSource()).isEqualTo("admin");
        }
    }
}
