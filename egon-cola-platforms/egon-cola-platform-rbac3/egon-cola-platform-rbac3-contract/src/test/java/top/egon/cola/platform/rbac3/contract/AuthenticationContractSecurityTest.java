package top.egon.cola.platform.rbac3.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationContractSecurityTest {

    @Test
    void bootstrapUserContainsIdentityBindingButNoCredentialOrSessionState() {
        BootstrapView.User user = new BootstrapView.User(
                "101", "tenant-a", "subject-a", "ACTIVE");

        assertEquals("subject-a", user.identitySub());
        assertEquals(List.of("id", "tenantId", "identitySub", "status"),
                java.util.Arrays.stream(BootstrapView.User.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
    }

    @Test
    void authorizationSnapshotRejectsInvalidIdentityAndTimeBounds() {
        Instant generated = Instant.parse("2026-08-14T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new UserAuthorizationSnapshot(
                "finance", "tenant-a", "subject-a", "101", 1L, 1L,
                List.of(), "sha256:x", generated, generated));
        assertThrows(IllegalArgumentException.class, () -> new UserAuthorizationSnapshot(
                "finance", "tenant-a", " ", "101", 1L, 1L,
                List.of(), "sha256:x", generated, generated.plusSeconds(1)));
    }
}
