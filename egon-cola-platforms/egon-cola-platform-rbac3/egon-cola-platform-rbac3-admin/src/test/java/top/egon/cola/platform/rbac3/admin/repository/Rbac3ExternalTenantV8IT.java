package top.egon.cola.platform.rbac3.admin.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Static V8 contract; live PostgreSQL rehearsal remains an operator gate. */
class Rbac3ExternalTenantV8IT {

    @Test
    void requiresVerifiedGateBeforeDestructiveTenantExternalization() throws IOException {
        String sql = migrationSql();
        String normalized = sql.toLowerCase();

        assertThat(normalized)
                .contains("current_setting(")
                .contains("rbac3.tenant_authority.gate_id")
                .contains("rbac3.tenant_authority.source_count")
                .contains("rbac3.tenant_authority.orphan_count")
                .contains("raise exception")
                .contains("create table rbac3_tenant_authorization_state")
                .contains("drop table rbac3_tenant");
        assertThat(normalized.indexOf("raise exception"))
                .isLessThan(normalized.indexOf(
                        "create table rbac3_tenant_authorization_state"
                ));
    }

    @Test
    void copiesPolicyStateAndRetargetsEveryTenantForeignKey() throws IOException {
        String normalized = migrationSql().toLowerCase();

        assertThat(normalized)
                .contains("insert into rbac3_tenant_authorization_state")
                .contains("select\n    id,\n    policy_version")
                .contains("pg_constraint")
                .contains("parent.relname = 'rbac3_tenant'")
                .contains("references rbac3_tenant_authorization_state(tenant_id)")
                .contains("zero remaining references")
                .contains("from rbac3_tenant_authorization_state");
    }

    @Test
    void migrationHistoryKeepsV1ToV7Immutable() throws IOException {
        assertThat(migrationSql()).isNotEmpty();
        assertThat(getClass().getClassLoader().getResourceAsStream(
                "db/migration/V1__create_rbac3_schema.sql"
        )).isNotNull();
        assertThat(getClass().getClassLoader().getResourceAsStream(
                "db/migration/V7__globalize_resource_catalog_and_remove_manifest.sql"
        )).isNotNull();
    }

    private static String migrationSql() throws IOException {
        try (var stream = Rbac3ExternalTenantV8IT.class.getClassLoader()
                .getResourceAsStream(
                        "db/migration/V8__externalize_tenant_authority.sql"
                )) {
            assertThat(stream).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
