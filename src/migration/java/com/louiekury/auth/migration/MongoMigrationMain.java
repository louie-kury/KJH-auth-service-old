package com.louiekury.auth.migration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.regex.Pattern;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.bson.Document;

/** 앱과 분리된 JavaExec 진입점. URI·자격증명과 서버 오류 원문을 출력하지 않는다. */
public final class MongoMigrationMain {

    private static final String CHANGELOG = "db/mongodb/changelog.xml";
    private static final Pattern AUTH_SOURCE_OPTION = Pattern.compile("(?:^|[&;])authsource=", Pattern.CASE_INSENSITIVE);

    private MongoMigrationMain() {
    }

    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        System.setProperty("liquibase.analytics.enabled", "false");
        System.setProperty("liquibase.changelogLockWaitTimeInMinutes", "1");
        System.setProperty("liquibase.changelogLockPollRate", "1");
        try {
            if (args.length != 1 || !("migrate".equals(args[0]) || "status".equals(args[0]))) {
                throw new IllegalArgumentException("명령은 migrate 또는 status입니다.");
            }
            String uri = resolveUrl(System.getenv("MONGODB_MIGRATION_URI"),
                System.getenv("MONGODB_MIGRATION_DATABASE"));
            Scope.child(Map.of(Scope.Attr.ui.name(), new LoggerUIService()), () -> run(args[0], uri));
        } catch (Exception exception) {
            System.err.println("MIGRATION_FAILED: DB 설정·구조·체크섬·권한·연결·락을 확인하세요. ("
                + exception.getClass().getSimpleName() + ")");
            System.exit(1);
        }
    }

    public static String resolveUrl(String uri, String database) {
        if (uri == null || uri.isBlank() || database == null
                || !database.matches("[A-Za-z0-9_-]{1,63}")
                || Set.of("admin", "config", "local", "test").contains(database.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("전용 URI와 명시적인 서비스 DB 이름이 필요합니다.");
        }
        ConnectionString parsed = new ConnectionString(uri);
        if (parsed.getDatabase() != null && !parsed.getDatabase().equals(database)) {
            throw new IllegalArgumentException("URI와 명시한 DB 이름이 다릅니다.");
        }
        if (parsed.getWriteConcern() != null && !parsed.getWriteConcern().isAcknowledged()) {
            throw new IllegalArgumentException("확인 가능한 쓰기 설정이 필요합니다.");
        }
        int queryAt = uri.indexOf('?');
        String address = queryAt < 0 ? uri : uri.substring(0, queryAt);
        String query = queryAt < 0 ? "" : uri.substring(queryAt + 1);
        int pathAt = address.indexOf('/', address.indexOf("://") + 3);
        String resolved = (pathAt < 0 ? address : address.substring(0, pathAt)) + "/" + database;
        if (parsed.getDatabase() == null && parsed.getCredential() != null
                && !AUTH_SOURCE_OPTION.matcher(query).find()) {
            query += (query.isEmpty() ? "" : "&") + "authSource="
                + URLEncoder.encode(parsed.getCredential().getSource(), StandardCharsets.UTF_8);
        }
        return resolved + (query.isEmpty() ? "" : "?" + query);
    }

    private static void run(String command, String uri) throws Exception {
        try (var resources = new ClassLoaderResourceAccessor();
                var database = DatabaseFactory.getInstance().openDatabase(uri, null, null, null, resources);
                var liquibase = new Liquibase(CHANGELOG, resources, database)) {
            initializeTrackingCollections(((MongoLiquibaseDatabase) database).getMongoDatabase());
            if ("migrate".equals(command)) {
                liquibase.update(new Contexts(), new LabelExpression());
                System.out.println("MIGRATION_COMPLETE");
            } else {
                liquibase.validate();
                var pending = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
                System.out.println("MIGRATION_PENDING=" + pending.size());
                for (var change : pending) {
                    System.out.println("PENDING " + change.getId());
                }
            }
        }
    }

    /** 최초 실행의 존재 확인/create 경합만 처리한다. 락 획득과 해제는 Liquibase가 담당한다. */
    private static void initializeTrackingCollections(MongoDatabase database) {
        for (String name : java.util.List.of("DATABASECHANGELOGLOCK", "DATABASECHANGELOG")) {
            try {
                database.createCollection(name);
            } catch (MongoCommandException exception) {
                if (exception.getErrorCode() != 48) {
                    throw exception;
                }
                Document existing = database.listCollections().filter(new Document("name", name)).first();
                if (existing == null || !"collection".equals(existing.getString("type"))) {
                    throw new IllegalStateException("Liquibase 메타데이터 컬렉션의 타입이 올바르지 않습니다.");
                }
            }
        }
        var locks = database.getCollection("DATABASECHANGELOGLOCK");
        for (Document existing : locks.find()) {
            if (!isValidLockDocument(existing)) {
                throw new IllegalStateException("Liquibase 기존 락 문서가 올바르지 않습니다.");
            }
        }
        try {
            if (!locks.insertOne(new Document("_id", 1).append("locked", false)
                    .append("lockGranted", new java.util.Date(0)).append("lockedBy", null)).wasAcknowledged()) {
                throw new IllegalStateException("Liquibase 락 초기화 쓰기를 확인할 수 없습니다.");
            }
        } catch (MongoWriteException exception) {
            if (exception.getError().getCode() != 11000) {
                throw exception;
            }
            Document existing = locks.find(new Document("_id", 1)).first();
            if (existing == null || !isValidLockDocument(existing)) {
                throw new IllegalStateException("Liquibase 기존 락 문서가 올바르지 않습니다.");
            }
            // 다른 프로세스가 가진 locked:true도 그대로 둔다. 덮어쓰거나 강제 해제하지 않는다.
        }
    }

    private static boolean isValidLockDocument(Document document) {
        return Integer.valueOf(1).equals(document.get("_id"))
            && document.get("locked") instanceof Boolean
            && document.get("lockGranted") instanceof java.util.Date
            && (document.get("lockedBy") == null || document.get("lockedBy") instanceof String);
    }
}
