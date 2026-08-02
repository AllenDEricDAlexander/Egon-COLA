package top.egon.cola.platform.rbac3.admin.snapshot.application;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemAuthorizationSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T04:00:00Z");

    @Test
    void snapshotContainsOnlyTheRequestedSystemAndIdentityBinding() {
        AuthorizationContextFacade.AuthorizationContext context =
                new AuthorizationContextFacade.AuthorizationContext(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 3, 11, false, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) -> context,
                (tenantId, sessionId) -> new AuthorizationDecisionService.SnapshotRecord(
                        tenantId, "alice-sub", "101", sessionSnapshot()),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var snapshot = service.snapshot("1", "5001", "gateway-admin", "alice-sub");

        assertThat(snapshot.identitySub()).isEqualTo("alice-sub");
        assertThat(snapshot.systemCode()).isEqualTo("gateway-admin");
        assertThat(snapshot.contextVersion()).isEqualTo(3);
        assertThat(snapshot.permissions()).containsExactly("gateway:release:read");
        assertThat(snapshot.permissions()).noneMatch(permission -> permission.startsWith("ddc:"));
    }

    @Test
    void snapshotRejectsAStoredIdentityFromAnotherIdpSubject() {
        AuthorizationContextFacade.AuthorizationContext context =
                new AuthorizationContextFacade.AuthorizationContext(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 3, 11, false, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) -> context,
                (tenantId, sessionId) -> new AuthorizationDecisionService.SnapshotRecord(
                        tenantId, "mallory-sub", "101", sessionSnapshot()),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.snapshot(
                "1", "5001", "gateway-admin", "alice-sub"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTHORIZATION_CONTEXT_MISMATCH");
    }

    @Test
    void unactivatedContextCanOnlyUseRbac3RoleActivationEndpoints() {
        AuthorizationContextFacade.AuthorizationContext context =
                new AuthorizationContextFacade.AuthorizationContext(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 0, 11, true, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) -> context,
                (tenantId, sessionId) -> {
                    throw new AssertionError("unactivated context must not load a snapshot");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.snapshot("1", "5001", "rbac3-admin", "alice-sub")
                .permissions()).containsExactlyInAnyOrder(
                "system:role-activation:read",
                "system:role-activation:use");
        assertThat(service.snapshot("1", "5001", "mock-backend", "alice-sub")
                .permissions()).isEmpty();
    }

    private SessionAuthorizationSnapshot sessionSnapshot() {
        return new SessionAuthorizationSnapshot(
                "5001", 7, 3, 11,
                List.of(
                        app("gateway-admin", Set.of("gateway:release:read")),
                        app("ddc-admin", Set.of("ddc:config:read"))),
                "sha256:all", NOW);
    }

    private AppAuthorizationContext app(String code, Set<String> permissions) {
        return new AppAuthorizationContext(
                code + "-id", code, List.of("role-1"), List.of("assignment-1"),
                List.of("role-1"), permissions, Map.of(), Map.of(), List.of(), null);
    }
}
