package top.egon.cola.platform.idp.admin.support.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdpClientTenantV5MigrationTest {

    private static final String V5_MIGRATION =
            "db/migration/V5__adopt_client_secrets_and_tenant_authority.sql";

    private static final Map<String, String> HISTORICAL_SHA256 = Map.of(
            "db/migration/V1__create_idp_schema.sql",
            "a2cad124faaf660b17e4079d97f69f0aab492e23285ce1ca74549c9c9b604f6c",
            "db/migration/V2__add_oauth_resource_servers.sql",
            "d4ed13713b20748f18729e7f27cafc800c1567f8bf5ebfaf06ed01c94d592c13",
            "db/migration/V3__create_transactional_outbox_schema.sql",
            "e9b7db01d4ddbad8c7c503815b53f8287b2c0faca90dd026394999156bb52e2a",
            "db/migration/V4__remove_user_token_version.sql",
            "888d5022a85dc5343b3513c4eac76067116ac4df6207d7e406497867115c8d80"
    );

    @Test
    void migratesV4ClientAndTenantFacts() throws IOException {
        String sql = migrationSql(V5_MIGRATION);

        assertTrue(sql.contains("create table identity_tenant"));
        assertTrue(sql.contains("create table identity_tenant_membership"));
        assertTrue(sql.contains("create table identity_client_secret"));
        assertTrue(sql.contains("alter table identity_client add column app_id"));
        assertTrue(sql.contains("grant_context"));
        assertTrue(sql.contains("drop table identity_client_jwk"));
        assertTrue(sql.contains(
                "drop column admission_ticket_ttl_seconds"
        ));
        assertTrue(sql.contains("initializing"));
        assertTrue(sql.contains("migrating-"));
    }

    @Test
    void rejectsDuplicateAppOrActiveSecret() throws IOException {
        String sql = migrationSql(V5_MIGRATION);

        assertTrue(sql.contains(
                "uq_identity_client_confidential_app_id"
        ));
        assertTrue(sql.contains("uq_identity_client_active_secret"));
        assertTrue(sql.contains("uq_identity_tenant_member"));
        assertTrue(sql.contains("grant_context = 'tenant'"));
        assertTrue(sql.contains("grant_context = 'platform'"));
        assertTrue(sql.contains("grant_context is null"));
        assertTrue(sql.contains("tenant_id is null"));
        assertTrue(sql.contains("jsonb_array_length(allowed_scopes) > 0"));
    }

    @Test
    void preservesHistoricalChecksums() throws IOException {
        HISTORICAL_SHA256.forEach((migration, expected) -> {
            try {
                assertEquals(expected, sha256(migration), migration);
            } catch (IOException exception) {
                throw new AssertionError(exception);
            }
        });
    }

    private String migrationSql(String migration) throws IOException {
        try (InputStream input = resource(migration)) {
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            ).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
    }

    private InputStream resource(String migration) {
        InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(migration);
        assertNotNull(input, migration);
        return input;
    }

    private String sha256(String migration) throws IOException {
        try (InputStream input = resource(migration)) {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.readAllBytes());
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }
}
