package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidate;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3DevelopmentAuthorizationContextInitializerTest {

    private static final Instant NOW = Instant.parse("2026-08-03T02:00:00Z");

    @Test
    void activatesAllFiveDevelopmentRolesByApplicationAndRoleCode() {
        AtomicReference<RoleActivationFacade.ReplaceCommand> captured =
                new AtomicReference<>();
        var initializer = new Rbac3DevelopmentAuthorizationContextInitializer(
                true,
                (tenantId, userId, now) -> candidates(
                        application("rbac3-admin",
                                candidate("201", "RBAC3_LOCAL_ADMIN", "PASSWORD")),
                        application("idp-admin",
                                candidate("202", "IDP_LOCAL_ADMIN", "PASSWORD")),
                        application("gateway-admin",
                                candidate("203", "GATEWAY_LOCAL_ADMIN", "PASSWORD")),
                        application("ddc-admin",
                                candidate("204", "DDC_LOCAL_ADMIN", "PASSWORD")),
                        application("mock-backend",
                                candidate("205", "MOCK_LOCAL_ADMIN", "PASSWORD"),
                                candidate("206", "MOCK_LOCAL_ENTRY", "PASSWORD"))),
                captured::set);

        initializer.initialize(context(), NOW);

        assertThat(captured.get().requestedRoleIds())
                .containsExactly("201", "202", "203", "204", "205", "206");
    }

    @Test
    void activatesOnlyPasswordStrengthRolesFromTheDevelopmentTopology() {
        AtomicReference<RoleActivationFacade.ReplaceCommand> captured =
                new AtomicReference<>();
        var initializer = new Rbac3DevelopmentAuthorizationContextInitializer(
                true,
                (tenantId, userId, now) -> candidates(
                        application("idp-admin",
                                candidate("201", "IDP_LOCAL_ADMIN", "PASSWORD"),
                                candidate("203", "UNRELATED_ROLE", "PASSWORD")),
                        application("rbac3-admin",
                                candidate("202", "RBAC3_LOCAL_ADMIN", "STRONG"))),
                captured::set);

        var initialized = initializer.initialize(context(), NOW);

        assertThat(initialized).isEqualTo(
                SystemAuthorizationSnapshotService.ContextInitialization.COMPLETED);
        assertThat(captured.get().tenantId()).isEqualTo("1");
        assertThat(captured.get().identitySub()).isEqualTo("alice-sub");
        assertThat(captured.get().userId()).isEqualTo("101");
        assertThat(captured.get().sessionId()).isEqualTo("5001");
        assertThat(captured.get().expectedContextVersion()).isZero();
        assertThat(captured.get().requestedRoleIds()).containsExactly("201");
        assertThat(captured.get().actorId()).isEqualTo("development-bootstrap");
    }

    @Test
    void sameDevelopmentRoleCodeInAnotherApplicationIsNotActivated() {
        AtomicReference<RoleActivationFacade.ReplaceCommand> captured =
                new AtomicReference<>();
        var initializer = new Rbac3DevelopmentAuthorizationContextInitializer(
                true,
                (tenantId, userId, now) -> candidates(
                        application("idp-admin",
                                candidate("201", "IDP_LOCAL_ADMIN", "PASSWORD")),
                        application("mock-backend",
                                candidate("202", "IDP_LOCAL_ADMIN", "PASSWORD"))),
                captured::set);

        initializer.initialize(context(), NOW);

        assertThat(captured.get().requestedRoleIds()).containsExactly("201");
    }

    @Test
    void scopesActivationCommandIdByTenantWhenTheIdpSessionIsShared() {
        List<String> commandIds = new ArrayList<>();
        var initializer = new Rbac3DevelopmentAuthorizationContextInitializer(
                true,
                (tenantId, userId, now) -> candidates(application(
                        "idp-admin",
                        candidate("201", "IDP_LOCAL_ADMIN", "PASSWORD"))),
                command -> commandIds.add(command.commandId()));

        initializer.initialize(context("1"), NOW);
        initializer.initialize(context("2"), NOW);

        assertThat(commandIds).containsExactly(
                "development-bootstrap:auto-activate-local-admin:1:5001:0",
                "development-bootstrap:auto-activate-local-admin:2:5001:0");
    }

    @Test
    void disabledInitializerLeavesTheAuthorizationContextUntouched() {
        AtomicBoolean queried = new AtomicBoolean();
        var initializer = new Rbac3DevelopmentAuthorizationContextInitializer(
                false,
                (tenantId, userId, now) -> {
                    queried.set(true);
                    return candidates(application("idp-admin",
                            candidate("201", "IDP_LOCAL_ADMIN", "PASSWORD")));
                },
                command -> {
                    throw new AssertionError("disabled initializer must not activate roles");
                });

        assertThat(initializer.initialize(context(), NOW)).isEqualTo(
                SystemAuthorizationSnapshotService.ContextInitialization.UNCHANGED);
        assertThat(queried).isFalse();
    }

    @Test
    void concurrentActivationIsTreatedAsAnAlreadyInitializedContext() {
        var initializer = new Rbac3DevelopmentAuthorizationContextInitializer(
                true,
                (tenantId, userId, now) -> candidates(application(
                        "idp-admin",
                        candidate("201", "IDP_LOCAL_ADMIN", "PASSWORD"))),
                command -> {
                    throw new Rbac3RuleViolation("ROLE_ACTIVATION_VERSION_CONFLICT");
                });

        assertThat(initializer.initialize(context(), NOW)).isEqualTo(
                SystemAuthorizationSnapshotService.ContextInitialization.CONCURRENT);
    }

    private AuthorizationContextFacade.AuthorizationContext context() {
        return context("1");
    }

    private AuthorizationContextFacade.AuthorizationContext context(String tenantId) {
        return new AuthorizationContextFacade.AuthorizationContext(
                "9001", tenantId, "5001", "alice-sub", "101",
                7, 0, 11, true, "ACTIVE", NOW.minusSeconds(10),
                NOW.plusSeconds(3600));
    }

    private RoleActivationCandidateView candidates(
            RoleActivationCandidateView.ApplicationCandidates... applications) {
        return new RoleActivationCandidateView(
                List.of(applications),
                7, 11, "directory-1", List.of(), NOW);
    }

    private RoleActivationCandidateView.ApplicationCandidates application(
            String code,
            RoleActivationCandidate... candidates) {
        return new RoleActivationCandidateView.ApplicationCandidates(
                "application-" + code, code, List.of(candidates));
    }

    private RoleActivationCandidate candidate(
            String id,
            String code,
            String requiredStrength) {
        return new RoleActivationCandidate(
                id, code, code, List.of(id), List.of("assignment-" + id),
                List.of(), "MEDIUM", requiredStrength, null);
    }
}
