package top.egon.cola.platform.rbac3.admin.runtime.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.repository.InitialAuthorizationContextRepository.InitialAuthorizationContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemAuthorizationSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");

    @Test
    void exposesOnlyRoleActivationPermissionsBeforeFirstRuntimeSnapshot() {
        var service = service();

        var snapshot = service.snapshot("1", "alice-sub", "rbac3-admin");

        assertThat(snapshot.rbac3UserId()).isEqualTo("101");
        assertThat(snapshot.activeRoleIds()).isEmpty();
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder(
                "system:role-activation:read",
                "system:role-activation:use"
        );
        assertThat(snapshot.authVersion()).isEqualTo(1L);
        assertThat(snapshot.policyVersion()).isEqualTo(2L);
    }

    @Test
    void keepsOtherSystemsFailClosedBeforeFirstRuntimeSnapshot() {
        var service = service();

        assertThatThrownBy(() -> service.snapshot(
                "1", "alice-sub", "idp-admin"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessage("AUTH_SNAPSHOT_NOT_READY");
    }

    private SystemAuthorizationSnapshotService service() {
        return new SystemAuthorizationSnapshotService(
                (tenantId, identitySub) -> {
                    throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
                },
                (tenantId, identitySub) -> Optional.of(
                        new InitialAuthorizationContext("101", 1L, 2L)),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
