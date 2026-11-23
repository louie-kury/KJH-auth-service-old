package com.louiekury.auth.migration;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "AUTH_TEST_MONGODB_URI", matches = ".+")
class MongoMigrationIntegrationTest {
    private static final String HISTORY = "DATABASECHANGELOG";
    private static final String LOCK = "DATABASECHANGELOGLOCK";
    private static final String CHANGELOG = "db/mongodb/changelog.xml";
    private static final String PROBE = "migration_probe";

    @TempDir
    Path temporaryDirectory;

    private final List<String> ownedDatabases = new ArrayList<>();
    private MongoClient client;
    private String uri;
    private String classpath;

    @BeforeEach
    void connectToIsolatedMongo() throws Exception {
        uri = System.getenv("AUTH_TEST_MONGODB_URI");
        ConnectionString parsed = new ConnectionString(uri);
        assertThat(uri).startsWith("mongodb://127.0.0.1:");
        assertThat(parsed.getHosts()).hasSize(1);
        assertThat(parsed.getDatabase()).isNull();
        client = MongoClients.create(uri);

        Path changelog = temporaryDirectory.resolve(CHANGELOG);
        Files.createDirectories(changelog.getParent());
        Files.writeString(changelog, """
            <?xml version="1.0" encoding="UTF-8"?>
            <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:mongo="http://www.liquibase.org/xml/ns/mongodb"
                xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd http://www.liquibase.org/xml/ns/mongodb https://www.liquibase.org/xml/ns/mongodb/liquibase-mongodb-latest.xsd">
                <changeSet id="test-probe" author="auth-test">
                    <mongo:runCommand>
                        <mongo:command>{"create":"migration_probe"}</mongo:command>
                    </mongo:runCommand>
                </changeSet>
            </databaseChangeLog>
            """);
        String migrationClasspath = System.getProperty("auth.mongoMigrationClasspath");
        assertThat(migrationClasspath).as("Gradle migration runtime classpath").isNotBlank();
        List<String> entries = Arrays.asList(migrationClasspath.split(Pattern.quote(File.pathSeparator)));
        assertThat(entries).anyMatch(entry -> Files.exists(Path.of(entry).resolve(CHANGELOG)));
        List<String> withFixture = new ArrayList<>();
        withFixture.add(temporaryDirectory.toString());
        entries.stream().filter(entry -> !Files.exists(Path.of(entry).resolve(CHANGELOG))).forEach(withFixture::add);
        classpath = String.join(File.pathSeparator, withFixture);
    }

    @AfterEach
    void dropTestDatabases() {
        if (client != null) {
            try (MongoClient connection = client) {
                for (String name : ownedDatabases) {
                    connection.getDatabase(name).drop();
                }
            }
        }
    }

    @Test
    void appliesOnceAndReportsPendingChangesForTheExplicitDatabase() throws Exception {
        MongoDatabase database = freshDatabase();
        MongoDatabase otherDatabase = freshDatabase();

        Run pending = run("status", uri, database.getName());
        assertSuccess(pending, "MIGRATION_PENDING=1");
        assertThat(database.listCollectionNames()).doesNotContain(PROBE);

        assertSuccess(run("migrate", uri, database.getName()), "MIGRATION_COMPLETE");
        assertThat(database.listCollectionNames()).contains(PROBE);
        Document applied = database.getCollection(HISTORY).find().first();
        assertThat(applied).isNotNull();
        assertThat(applied.getString("id")).isEqualTo("test-probe");
        assertThat(applied.getString("md5sum")).isNotBlank();

        assertSuccess(run("migrate", uri, database.getName()), "MIGRATION_COMPLETE");
        assertSuccess(run("status", uri, database.getName()), "MIGRATION_PENDING=0");
        assertThat(database.getCollection(HISTORY).find()).containsExactly(applied);
        assertThat(otherDatabase.listCollectionNames()).isEmpty();
        assertReleasedLock(database);
    }

    @Test
    void concurrentRunnersKeepOneHistoryEntryAndReleaseTheLock() throws Exception {
        MongoDatabase database = freshDatabase();
        Running one = start("migrate", uri, database.getName());
        Running two = start("migrate", uri, database.getName());
        try {
            assertSuccess(finish(one), "MIGRATION_COMPLETE");
            assertSuccess(finish(two), "MIGRATION_COMPLETE");
            assertThat(database.getCollection(HISTORY).countDocuments()).isEqualTo(1);
            assertReleasedLock(database);
        } finally {
            one.process().destroyForcibly();
            two.process().destroyForcibly();
        }
    }

    @Test
    void rejectsMissingOrMismatchedDatabaseWithoutChangingEitherDatabase() throws Exception {
        MongoDatabase named = freshDatabase();
        MongoDatabase path = freshDatabase();

        Run missing = run("migrate", uri + "/" + path.getName(), null);
        Run mismatch = run("migrate", uri + "/" + path.getName(), named.getName());
        for (Run invalid : List.of(missing, mismatch)) {
            assertThat(invalid.exitCode()).isNotZero();
            assertThat(invalid.output()).contains("MIGRATION_FAILED").doesNotContain(uri, path.getName(), named.getName());
        }
        assertThat(named.listCollectionNames()).isEmpty();
        assertThat(path.listCollectionNames()).isEmpty();
    }

    private MongoDatabase freshDatabase() {
        String name = "auth_migration_it_" + UUID.randomUUID().toString().replace("-", "");
        ownedDatabases.add(name);
        return client.getDatabase(name);
    }

    private Running start(String command, String connection, String database) throws Exception {
        Path output = Files.createTempFile(temporaryDirectory, "migration-", ".log");
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8", "-cp", classpath,
            "com.louiekury.auth.migration.MongoMigrationMain", command)
            .redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().put("MONGODB_MIGRATION_URI", connection);
        if (database == null) {
            builder.environment().remove("MONGODB_MIGRATION_DATABASE");
        } else {
            builder.environment().put("MONGODB_MIGRATION_DATABASE", database);
        }
        return new Running(builder.start(), output);
    }

    private Run run(String command, String connection, String database) throws Exception {
        return finish(start(command, connection, database));
    }

    private static Run finish(Running running) throws Exception {
        if (!running.process().waitFor(55, TimeUnit.SECONDS)) {
            running.process().destroyForcibly();
            throw new AssertionError("Migration process timed out: " + Files.readString(running.output()));
        }
        return new Run(running.process().exitValue(), Files.readString(running.output()));
    }

    private static void assertSuccess(Run run, String marker) {
        assertThat(run.exitCode()).as(run.output()).isZero();
        assertThat(run.output()).contains(marker);
    }

    private static void assertReleasedLock(MongoDatabase database) {
        assertThat(database.getCollection(LOCK).countDocuments()).isEqualTo(1);
        Document lock = database.getCollection(LOCK).find().first();
        assertThat(lock).isNotNull();
        assertThat(lock.getBoolean("locked")).isFalse();
    }

    private static final class Running {
        private final Process process;
        private final Path output;

        private Running(Process process, Path output) {
            this.process = process;
            this.output = output;
        }

        private Process process() {
            return process;
        }

        private Path output() {
            return output;
        }
    }

    private static final class Run {
        private final int exitCode;
        private final String output;

        private Run(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        private int exitCode() {
            return exitCode;
        }

        private String output() {
            return output;
        }
    }
}
