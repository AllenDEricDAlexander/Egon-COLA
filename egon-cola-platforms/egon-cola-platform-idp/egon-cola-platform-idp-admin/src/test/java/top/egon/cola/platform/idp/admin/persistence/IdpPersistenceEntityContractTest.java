package top.egon.cola.platform.idp.admin.persistence;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.audit.domain.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientAudienceEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.support.outbox.domain.pojo.IdentityOutboxEventEntity;
import top.egon.cola.platform.idp.admin.token.domain.IdentitySigningKeyEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpPersistenceEntityContractTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void clientRequiresPkceAndKeepsExactRedirectAndAudienceValues() {
        IdentityClientEntity client = IdentityClientEntity.createPublic(
                "gateway-admin",
                "Gateway Admin",
                900,
                604_800,
                NOW
        );
        IdentityClientRedirectUriEntity redirect =
                IdentityClientRedirectUriEntity.create(
                        "redirect-1",
                        client.getClientId(),
                        "http://127.0.0.1:18080/callback",
                        NOW
                );
        IdentityClientAudienceEntity audience =
                IdentityClientAudienceEntity.create(
                        "audience-1",
                        client.getClientId(),
                        "gateway-admin-api",
                        NOW
                );

        assertEquals(IdentityClientEntity.Status.ACTIVE, client.getStatus());
        assertEquals(true, client.isPkceRequired());
        assertEquals("http://127.0.0.1:18080/callback",
                redirect.getRedirectUri());
        assertEquals("gateway-admin-api", audience.getAudience());
        assertThrows(IllegalArgumentException.class, () ->
                IdentityClientRedirectUriEntity.create(
                        "redirect-2",
                        client.getClientId(),
                        "http://127.0.0.1:18080/callback#fragment",
                        NOW
                ));
    }

    @Test
    void signingAuditAndOutboxRecordsStartInSafeStates() {
        IdentitySigningKeyEntity key = IdentitySigningKeyEntity.published(
                "key-1",
                "encrypted-private-key",
                "{\"kty\":\"RSA\"}",
                NOW
        );
        IdentityAuditLogEntity audit = IdentityAuditLogEntity.record(
                "audit-1",
                "IDENTITY_BOOTSTRAPPED",
                null,
                "42",
                "SUCCESS",
                "BOOTSTRAP_CLI",
                "{}",
                NOW
        );
        IdentityOutboxEventEntity outbox = IdentityOutboxEventEntity.pending(
                "event-1",
                "IDENTITY",
                "42",
                "IDENTITY_BOOTSTRAPPED",
                "{\"identitySub\":\"42\"}",
                NOW
        );

        assertEquals(IdentitySigningKeyEntity.Status.PUBLISHED,
                key.getStatus());
        assertEquals("RS256", key.getAlgorithm());
        assertEquals("SUCCESS", audit.getResult());
        assertEquals(IdentityOutboxEventEntity.Status.PENDING,
                outbox.getStatus());
        assertEquals(NOW, outbox.getNextAttemptAt());
    }
}
