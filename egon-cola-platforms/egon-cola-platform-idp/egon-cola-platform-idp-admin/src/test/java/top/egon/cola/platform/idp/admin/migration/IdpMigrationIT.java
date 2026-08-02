package top.egon.cola.platform.idp.admin.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdpMigrationIT {

    private static final String MIGRATION =
            "db/migration/V1__create_idp_schema.sql";

    @Test
    void migrationCreatesIdentityTablesButNoAuthorizationTables()
            throws IOException {
        String sql = migrationSql();

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
        String sql = migrationSql();

        assertTrue(sql.contains("unique (username_normalized)"));
        assertTrue(sql.contains("unique (identity_sub, credential_type)"));
        assertTrue(sql.contains("unique (client_id, redirect_uri)"));
        assertTrue(sql.contains("unique (client_id, audience)"));
    }

    private void assertCreates(String sql, String table) {
        assertTrue(sql.contains("create table " + table), table);
    }

    private String migrationSql() throws IOException {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(MIGRATION)) {
            assertNotNull(input, MIGRATION);
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            ).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
    }
}
