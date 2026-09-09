package top.egon.cola.platform.tianquan.shoubing.admin.support.persistence;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.pojo.IdentityAuditLogEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.domain.pojo.IdentityOutboxEventEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.pojo.IdentitySigningKeyEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpPersistenceEntityContractTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void clientRequiresPkceAndKeepsExactRedirectValue() {
        IdentityClientEntity client = IdentityClientEntity.createPublic(
                "yuheng-admin",
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
        assertEquals(IdentityClientEntity.Status.ACTIVE, client.getStatus());
        assertEquals(true, client.isPkceRequired());
        assertEquals("http://127.0.0.1:18080/callback",
                redirect.getRedirectUri());
        assertThrows(IllegalArgumentException.class, () ->
                IdentityClientRedirectUriEntity.create(
                        "redirect-2",
                        client.getClientId(),
                        "http://127.0.0.1:18080/callback#fragment",
                        NOW
                ));
    }

    @Test
    void resourceAndUserGrantKeepApplicationBoundary() {
        IdentityResourceServerEntity resource =
                IdentityResourceServerEntity.create(
                        "resource-row-1",
                        "permission-tianquan-shoubing-local",
                        "https://api.egon.internal/local/permission/tianquan-shoubing",
                        "permission",
                        "tianquan-shoubing",
                        "local",
                        "Tianquan-Shoubing Local",
                        "tianquan-shoubing-admin-web",
                        "tianquan-shoubing",
                        "tianquan-shoubing:access",
                        300,
                        IdentityResourceServerEntity.Status.ACTIVE,
                        NOW
                );
        IdentityClientResourceGrantEntity grant =
                IdentityClientResourceGrantEntity.userDelegation(
                        "grant-row-1",
                        "tianquan-shoubing-admin-web",
                        resource.getResourceServerId(),
                        NOW
                );

        assertEquals("permission-tianquan-shoubing-local", resource.getResourceServerId());
        assertEquals("tianquan-shoubing", resource.getAppCode());
        assertEquals(
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION,
                grant.getGrantType()
        );
        assertEquals("[]", grant.getAllowedScopes());
        assertEquals(null, grant.getTenantId());
    }

    @Test
    void serviceGrantRequiresTenantAndNonEmptyScopes() {
        IdentityClientResourceGrantEntity grant =
                IdentityClientResourceGrantEntity.clientCredentials(
                        "grant-row-2",
                        "tianquan-shoubing-service",
                        "permission-tianquan-jianshen-local",
                        "tenant-1",
                        "[\"tianquan-jianshen:policy:read\"]",
                        NOW
                );

        assertEquals("tenant-1", grant.getTenantId());
        assertEquals("[\"tianquan-jianshen:policy:read\"]", grant.getAllowedScopes());
        assertThrows(IllegalArgumentException.class, () ->
                IdentityClientResourceGrantEntity.clientCredentials(
                        "grant-row-3",
                        "tianquan-shoubing-service",
                        "permission-tianquan-jianshen-local",
                        null,
                        "[]",
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
