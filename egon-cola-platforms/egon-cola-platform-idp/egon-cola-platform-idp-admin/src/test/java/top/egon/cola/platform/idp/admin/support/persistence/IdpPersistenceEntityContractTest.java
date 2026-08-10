package top.egon.cola.platform.idp.admin.support.persistence;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.audit.domain.pojo.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientJwkEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.support.outbox.domain.pojo.IdentityOutboxEventEntity;
import top.egon.cola.platform.idp.admin.token.domain.pojo.IdentitySigningKeyEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpPersistenceEntityContractTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void clientRequiresPkceAndKeepsExactRedirectValue() {
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
    void resourceCredentialAndUserGrantKeepApplicationBoundary() {
        IdentityResourceServerEntity resource =
                IdentityResourceServerEntity.create(
                        "resource-row-1",
                        "permission-idp-local",
                        "https://api.egon.internal/local/permission/idp",
                        "permission",
                        "idp",
                        "local",
                        "IdP Local",
                        "idp-admin-web",
                        "idp",
                        "idp:access",
                        300,
                        IdentityResourceServerEntity.Status.ACTIVE,
                        NOW
                );
        IdentityClientJwkEntity credential = IdentityClientJwkEntity.create(
                "jwk-row-1",
                "idp-admin-web",
                "idp-local-2026-08",
                "{\"kty\":\"RSA\"}",
                NOW,
                NOW.plusSeconds(3600),
                NOW
        );
        IdentityClientResourceGrantEntity grant =
                IdentityClientResourceGrantEntity.userDelegation(
                        "grant-row-1",
                        "idp-admin-web",
                        resource.getResourceServerId(),
                        NOW
                );

        assertEquals("permission-idp-local", resource.getResourceServerId());
        assertEquals("idp", resource.getAppCode());
        assertEquals("RS256", credential.getAlgorithm());
        assertEquals(IdentityClientJwkEntity.Status.ACTIVE,
                credential.getStatus());
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
                        "idp-service",
                        "permission-rbac3-local",
                        "tenant-1",
                        "[\"rbac3:policy:read\"]",
                        NOW
                );

        assertEquals("tenant-1", grant.getTenantId());
        assertEquals("[\"rbac3:policy:read\"]", grant.getAllowedScopes());
        assertThrows(IllegalArgumentException.class, () ->
                IdentityClientResourceGrantEntity.clientCredentials(
                        "grant-row-3",
                        "idp-service",
                        "permission-rbac3-local",
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
