package top.egon.cola.platform.idp.admin.support.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdpMigrationIT {

    private static final String V1_MIGRATION =
            "db/migration/V1__create_idp_schema.sql";
    private static final String V2_MIGRATION =
            "db/migration/V2__add_oauth_resource_servers.sql";

    @Test
    void migrationCreatesIdentityTablesButNoAuthorizationTables()
            throws IOException {
        String sql = migrationSql(V1_MIGRATION);

        assertCreates(sql, "identity_user");
        assertCreates(sql, "identity_user_credential");
        assertCreates(sql, "identity_client");
        assertCreates(sql, "identity_client_redirect_uri");
        assertCreates(sql, "identity_client_audience");
        assertCreates(sql, "identity_signing_key");
        assertCreates(sql, "identity_audit_log");
        assertCreates(sql, "identity_outbox_event");
        assertFalse(sql.contains("create table tenant"));
        assertFalse(sql.contains("create table tenant_membership"));
        assertFalse(sql.contains("create table role"));
        assertFalse(sql.contains("create table permission"));
        assertFalse(sql.contains("create table identity_session"));
    }

    @Test
    void migrationDefinesGlobalUsernameClientAndCredentialUniqueness()
            throws IOException {
        String sql = migrationSql(V1_MIGRATION);

        assertTrue(sql.contains("unique (username_normalized)"));
        assertTrue(sql.contains("unique (identity_sub, credential_type)"));
        assertTrue(sql.contains("unique (client_id, redirect_uri)"));
        assertTrue(sql.contains("unique (client_id, audience)"));
    }

    @Test
    void v2CreatesResourceCredentialAndGrantTablesAndDropsAudience()
            throws IOException {
        String sql = migrationSql(V2_MIGRATION);

        assertCreates(sql, "identity_resource_server");
        assertCreates(sql, "identity_client_jwk");
        assertCreates(sql, "identity_client_resource_grant");
        assertTrue(sql.contains("drop table identity_client_audience"));
        assertTrue(sql.contains("unique (resource_uri)"));
        assertTrue(sql.contains(
                "unique (biz_code, app_code, environment)"
        ));
        assertTrue(sql.contains("unique (client_id, kid)"));
        assertTrue(sql.contains(
                "where grant_type = 'user_delegation'"
        ));
        assertTrue(sql.contains(
                "where grant_type = 'client_credentials'"
        ));
    }

    @Test
    void v2ConstrainsGrantTypeTenantAndScopeShape() throws IOException {
        String sql = migrationSql(V2_MIGRATION);

        assertTrue(sql.contains(
                "grant_type in ('user_delegation', 'client_credentials')"
        ));
        assertTrue(sql.contains(
                "grant_type = 'user_delegation' and tenant_id is null"
        ));
        assertTrue(sql.contains("allowed_scopes = '[]'::jsonb"));
        assertTrue(sql.contains(
                "grant_type = 'client_credentials' and tenant_id is not null"
        ));
        assertTrue(sql.contains("jsonb_array_length(allowed_scopes) > 0"));
    }

    private void assertCreates(String sql, String table) {
        assertTrue(sql.contains("create table " + table), table);
    }

    private String migrationSql(String migration) throws IOException {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(migration)) {
            assertNotNull(input, migration);
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            ).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
    }
}
