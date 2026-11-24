package com.louiekury.auth.global;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IdentityMappingContractTest {
    @Test
    void mapsOnlyTheColumnsPresentInTheIdentitySchema() throws Exception {
        Class<?> account = Class.forName("com.louiekury.auth.account.adapter.out.persistence.AccountJpaEntity");
        Class<?> credential = Class.forName("com.louiekury.auth.account.adapter.out.persistence.LoginCredentialJpaEntity");
        var builder = new StandardServiceRegistryBuilder();
        builder.applySettings(Map.of(
            "hibernate.dialect", "org.hibernate.dialect.MySQLDialect",
            "hibernate.boot.allow_jdbc_metadata_access", false,
            "hibernate.hbm2ddl.auto", "none"));
        try (var registry = builder.build()) {
            var metadata = new MetadataSources(registry)
                .addAnnotatedClass(account)
                .addAnnotatedClass(credential)
                .buildMetadata();
            Table accountTable = metadata.getEntityBinding(account.getName()).getTable();
            Table credentialTable = metadata.getEntityBinding(credential.getName()).getTable();

            assertThat(accountTable.getName()).isEqualTo("account");
            assertThat(columnNames(accountTable)).containsExactlyInAnyOrderElementsOf(Set.of(
                "id", "uuid", "status", "name", "phone_number", "gender", "birth_date", "address",
                "email", "terms_agreed", "created_at", "updated_at", "deleted_at"));
            assertThat(credentialTable.getName()).isEqualTo("login_credential");
            assertThat(columnNames(credentialTable)).containsExactlyInAnyOrderElementsOf(Set.of(
                "id", "account_id", "login_id", "password_hash", "created_at", "updated_at"));
            assertThat(column(credentialTable, "password_hash").isNullable()).isTrue();
            assertThat(column(credentialTable, "account_id").isNullable()).isFalse();
            assertThat(column(credentialTable, "login_id").getLength()).isEqualTo(100L);
            assertThat(column(accountTable, "uuid").getLength()).isEqualTo(36L);
            assertThat(column(accountTable, "email").getLength()).isEqualTo(254L);
        }
    }

    private static Set<String> columnNames(Table table) {
        return table.getColumns().stream().map(Column::getName).collect(Collectors.toSet());
    }

    private static Column column(Table table, String name) {
        return table.getColumns().stream().filter(column -> column.getName().equals(name)).findFirst().orElseThrow();
    }
}