package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.InitialAuthorizationContextRepository.InitialAuthorizationContext;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.controller.Rbac3AboutController;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.UserAuthorizationSnapshot;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemAuthorizationSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");

    @Test
    void businessOnlyActivationRetainsOnlyRbacSelfServicePermissions() {
        var snapshot = activatedService("mock-backend", true)
                .snapshot("1", "alice-sub", "tianquan-jianshen-admin");
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder(
                "system:about:read", "system:role-activation:read", "system:role-activation:use");
        assertThat(snapshot.activeRoleIds()).isEmpty();
        assertThat(snapshot.resourceCodes()).isEmpty();
        assertThat(snapshot.authVersion()).isEqualTo(1L);
        assertThat(snapshot.policyVersion()).isEqualTo(2L);
    }

    @Test
    void anActivatedRbacRoleCannotRemoveTheUsersOwnRoleSelectionEntry() {
        var snapshot = activatedService("tianquan-jianshen-admin", true)
                .snapshot("1", "alice-sub", "tianquan-jianshen-admin");
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder(
                "test:read", "system:about:read", "system:role-activation:read", "system:role-activation:use");
        assertThat(snapshot.activeRoleIds()).containsExactly("10");
    }

    @Test
    void businessActivationDoesNotBootstrapOtherSystemsOrMissingMemberships() {
        assertThatThrownBy(() -> activatedService("mock-backend", true)
                .snapshot("1", "alice-sub", "tianshu-admin"))
                .hasMessage("AUTHORIZATION_DENIED");
        assertThatThrownBy(() -> activatedService("mock-backend", true)
                .snapshot("1", "alice-sub", "tianquan-shoubing-admin"))
                .hasMessage("AUTHORIZATION_DENIED");
        assertThatThrownBy(() -> activatedService("mock-backend", false)
                .snapshot("1", "alice-sub", "tianquan-jianshen-admin"))
                .hasMessage("AUTHORIZATION_DENIED");
    }

    private SystemAuthorizationSnapshotService activatedService(String applicationCode, boolean member) {
        var app = new AppAuthorizationContext("5", applicationCode, List.of("10"), List.of("20"),
                List.of("10"), Set.of("test:read"), Map.of(), Map.of(), List.of(), null);
        var user = new UserAuthorizationSnapshot("tianquan-jianshen", "1", "alice-sub", "101", 1L, 2L,
                List.of(app), "checksum", NOW, NOW.plusSeconds(3600));
        return new SystemAuthorizationSnapshotService(
                (tenant, subject) -> new SnapshotRecordVO("1", "alice-sub", "101", user),
                (tenant, subject) -> member ? Optional.of(new InitialAuthorizationContext("101", 1L, 2L))
                        : Optional.empty(),
                Clock.fixed(NOW, ZoneOffset.UTC), true);
    }

    @Test
    void initialContextCanReadItsOwnAboutEndpointToRenderRoleSelection() throws Exception {
        String permission = Rbac3AboutController.class.getMethod("about")
                .getAnnotation(RequiresPermission.class).value();
        assertThat(service().snapshot("1", "alice-sub", "tianquan-jianshen-admin").permissions()).contains(permission);
    }

    @Test
    void exposesOnlySelfContextAndRoleActivationPermissionsBeforeFirstRuntimeSnapshot() {
        var service = service();

        var snapshot = service.snapshot("1", "alice-sub", "tianquan-jianshen-admin");

        assertThat(snapshot.rbac3UserId()).isEqualTo("101");
        assertThat(snapshot.activeRoleIds()).isEmpty();
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder(
                "system:about:read",
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
                "1", "alice-sub", "tianquan-shoubing-admin"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessage("AUTH_SNAPSHOT_NOT_READY");
    }

    @Test
    void staleSnapshotsDoNotFallBackToTheInitialContext() {
        var service = new SystemAuthorizationSnapshotService(
                (tenant, subject) -> { throw new Rbac3RuleViolation("POLICY_VERSION_MISMATCH"); },
                (tenant, subject) -> Optional.of(new InitialAuthorizationContext("101", 1L, 2L)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.snapshot("1", "alice-sub", "tianquan-jianshen-admin"))
                .hasMessage("POLICY_VERSION_MISMATCH");
    }

    @Test
    void keepsDdcFailClosedUnlessLocalBootstrapIsEnabled() {
        var service = service(false);

        assertThatThrownBy(() -> service.snapshot(
                "1", "alice-sub", "tianshu-admin"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessage("AUTH_SNAPSHOT_NOT_READY");
    }

    @Test
    void exposesDdcManagementPermissionsForLocalBootstrap() {
        var service = service(true);

        var snapshot = service.snapshot("1", "alice-sub", "tianshu-admin");

        assertThat(snapshot.rbac3UserId()).isEqualTo("101");
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder(
                "TIANSHU_READ", "TIANSHU_WRITE", "TIANSHU_PUBLISH", "TIANSHU_CACHE");
        assertThat(snapshot.authVersion()).isEqualTo(1L);
        assertThat(snapshot.policyVersion()).isEqualTo(2L);
    }

    private SystemAuthorizationSnapshotService service() {
        return service(false);
    }

    private SystemAuthorizationSnapshotService service(
            boolean ddcInitialContextEnabled) {
        return new SystemAuthorizationSnapshotService(
                (tenantId, identitySub) -> {
                    throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
                },
                (tenantId, identitySub) -> Optional.of(
                        new InitialAuthorizationContext("101", 1L, 2L)),
                Clock.fixed(NOW, ZoneOffset.UTC),
                ddcInitialContextEnabled
        );
    }
}
