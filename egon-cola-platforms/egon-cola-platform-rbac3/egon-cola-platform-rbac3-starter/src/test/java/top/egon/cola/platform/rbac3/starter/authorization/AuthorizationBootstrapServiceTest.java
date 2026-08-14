package top.egon.cola.platform.rbac3.starter.authorization;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationBootstrapServiceTest {

    @Test
    void exposesOnlyBoundIdentityAndSystemSnapshotFacts() {
        Instant now = Instant.parse("2026-08-02T04:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                "alice-sub", "tenant-a", "token-1",
                Set.of("gateway-admin-web"), now, now.plusSeconds(900),
                AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-a", "alice-sub", "101", "gateway-admin",
                3, 4, List.of("role-1"),
                Set.of("gateway:read", "gateway:groups:write"),
                Map.of(), Map.of(), "sha256:gateway", now, now.plusSeconds(900));
        var context = new AuthorizationService.RuntimeAuthorizationContext(
                identity, snapshot, false);

        var view = new AuthorizationBootstrapService(() -> context).current();

        assertThat(view.user().identitySub()).isEqualTo("alice-sub");
        assertThat(view.permissions()).containsExactlyInAnyOrder(
                "gateway:groups:write", "gateway:read");
        assertThat(view.authVersion()).isEqualTo(3);
    }
}
