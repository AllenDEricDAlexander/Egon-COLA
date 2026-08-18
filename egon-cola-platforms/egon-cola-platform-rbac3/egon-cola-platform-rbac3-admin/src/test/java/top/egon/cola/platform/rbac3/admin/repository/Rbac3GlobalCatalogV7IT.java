package top.egon.cola.platform.rbac3.admin.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Static migration contract used when the PostgreSQL IT environment is unavailable. */
class Rbac3GlobalCatalogV7IT {

    @Test
    void migrationSeparatesGlobalCatalogAndTenantEntitlement() throws IOException {
        String resource = "db/migration/V7__globalize_resource_catalog_and_remove_manifest.sql";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).isNotNull();
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase();
            assertThat(sql)
                    .contains("create table rbac3_tenant_application")
                    .contains("unique (tenant_id, application_id)")
                    .contains("drop table if exists rbac3_resource_manifest cascade")
                    .contains("uk_rbac3_application_code_global")
                    .contains("uk_rbac3_permission_code_global");
        }
    }
}
