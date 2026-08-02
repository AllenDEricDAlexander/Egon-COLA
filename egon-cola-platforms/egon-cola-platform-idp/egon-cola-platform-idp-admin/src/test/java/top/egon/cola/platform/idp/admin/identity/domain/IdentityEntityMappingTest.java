package top.egon.cola.platform.idp.admin.identity.domain;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityEntityMappingTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void userEntityRoundTripPreservesSecurityAndOptimisticVersions() {
        IdentityUser user = new IdentityUser(
                "1001",
                "Alice",
                "alice",
                "Alice A",
                IdentityUserStatus.LOCKED,
                7L,
                3,
                NOW.plusSeconds(300),
                NOW.minusSeconds(60),
                9L
        );

        IdentityUserEntity entity = IdentityUserEntity.fromDomain(user, NOW);

        assertEquals(user, entity.toDomain());
        assertEquals(NOW, entity.getCreatedAt());
        assertEquals(NOW, entity.getUpdatedAt());
    }

    @Test
    void credentialEntityRoundTripPreservesHashFlagsAndVersion() {
        PasswordCredential credential = new PasswordCredential(
                "1001",
                "{argon2}encoded",
                NOW,
                true,
                PasswordCredential.Status.ACTIVE,
                4L
        );

        IdentityCredentialEntity entity =
                IdentityCredentialEntity.fromDomain(credential, NOW);

        assertEquals(credential, entity.toDomain());
        assertEquals("1001", entity.getId());
    }
}
