package top.egon.cola.platform.rbac3.starter.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3UserDetailsLoaderTest {

    private static final Instant NOW = Instant.parse("2026-08-18T02:00:00Z");

    @Test
    void assemblesUserDetailsFromIdentityAndActiveSnapshot() {
        IdentityPrincipal identity = identity();
        SingleFlightSnapshotLoader snapshots = mock(SingleFlightSnapshotLoader.class);
        when(snapshots.load(identity)).thenReturn(snapshot(
                "alice-sub", "tenant-a"));

        Rbac3UserDetails details = new Rbac3UserDetailsLoader(snapshots)
                .load(identity);

        assertThat(details.identity()).isEqualTo(identity);
        assertThat(details.getUsername()).isEqualTo("alice-sub");
        assertThat(details.getPassword()).isNull();
        assertThat(details.permissions()).containsExactly("payment:read");
        assertThat(details.hasPermission("payment:read")).isTrue();
        assertThat(details.hasPermission("payment:write")).isFalse();
        assertThat(details.activeRoles()).singleElement()
                .satisfies(role -> {
                    assertThat(role.roleId()).isEqualTo("role-1");
                    assertThat(role.applicationCode()).isEqualTo("rbac3-admin");
                });
    }

    @Test
    void rejectsSnapshotBoundToAnotherIdentity() {
        IdentityPrincipal identity = identity();
        SingleFlightSnapshotLoader snapshots = mock(SingleFlightSnapshotLoader.class);
        when(snapshots.load(identity)).thenReturn(snapshot(
                "other-sub", "tenant-a"));

        assertThatThrownBy(() -> new Rbac3UserDetailsLoader(snapshots)
                .load(identity))
                .isInstanceOf(Rbac3AuthorizationClient.AuthorizationDeniedException.class);
    }

    private IdentityPrincipal identity() {
        return new IdentityPrincipal(
                "alice-sub", "tenant-a", "access-jti", Set.of("rbac3-admin"),
                NOW.minusSeconds(30), NOW.plusSeconds(300),
                AuthenticationContext.password());
    }

    private SystemAuthorizationSnapshot snapshot(
            String subject,
            String tenant) {
        return new SystemAuthorizationSnapshot(
                tenant, subject, "user-1", "rbac3-admin", 4L, 7L,
                List.of("role-1"), Set.of("payment:read"), Map.of(), Map.of(),
                "sha256:snapshot", NOW, NOW.plusSeconds(300));
    }
}
