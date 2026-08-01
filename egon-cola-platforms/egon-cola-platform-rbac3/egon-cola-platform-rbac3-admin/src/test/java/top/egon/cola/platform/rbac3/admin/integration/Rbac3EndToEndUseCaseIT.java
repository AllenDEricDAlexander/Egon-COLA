package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rbac3EndToEndUseCaseIT {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void childQualificationActivatesCanonicalRootAndAuthorizesWholeFamily() {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(role("root"), role("cashier"), role("auditor")),
                List.of(new RoleEdge("root", "cashier"),
                        new RoleEdge("root", "auditor")));
        var activation = new DefaultRoleActivationResolver().resolve(
                new RoleActivationInput(
                        "tenant", "user", "session", List.of("cashier"),
                        List.of(new EligibleAssignmentFact(
                                "assignment", "user", "cashier",
                                EligibleAssignmentFact.Status.ACTIVE,
                                NOW.minusSeconds(60), null)),
                        hierarchy, List.of(),
                        new AuthorizationRuleFacts(
                                List.of(new AuthorizationRuleFacts.PermissionBinding(
                                        "auditor", "payment:read")),
                                List.of(), List.of(), List.of(), List.of()),
                        3L, 5L, 7L, NOW));

        SessionAuthorizationSnapshot snapshot = new SessionAuthorizationSnapshot(
                "session", activation.snapshot().authVersion(),
                activation.snapshot().sessionVersion(),
                activation.snapshot().policyVersion(),
                List.of(new AppAuthorizationContext(
                        "application", "finance", List.copyOf(
                                activation.activeRoleSet().rootIds()),
                        activation.eligibleAssignmentIds(), List.copyOf(
                                activation.snapshot().effectiveRoleIds()),
                        activation.snapshot().permissionCodes(), Map.of(), Map.of(),
                        List.of(), activation.snapshot().landingRouteCode())),
                activation.snapshot().checksum(), NOW);
        AuthorizationDecisionService decisions = new AuthorizationDecisionService(
                (tenant, session) -> new AuthorizationDecisionService.SnapshotRecord(
                        tenant, "user", snapshot),
                (tenant, session) -> false,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = decisions.decide(
                servicePrincipal("finance"),
                request("finance", "payment:read"));

        assertThat(activation.activeRoleSet().rootIds()).containsExactly("root");
        assertThat(activation.snapshot().effectiveRoleIds())
                .containsExactly("auditor", "cashier", "root");
        assertThat(result.functionDecision().decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.functionDecision().evidenceIds())
                .containsExactly("auditor", "cashier", "root");

        assertThatThrownBy(() -> decisions.decide(
                servicePrincipal("inventory"),
                request("finance", "payment:read")))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("APPLICATION_BINDING_DENIED"));
    }

    private static AuthorizationDecisionService.DecisionRequest request(
            String application,
            String permission) {
        return new AuthorizationDecisionService.DecisionRequest(
                new AuthorizationDecisionService.Subject("tenant", "user", "session"),
                permission,
                new AuthorizationDecisionService.Resource(application, "payment"),
                EnumSet.of(AuthorizationDecisionService.DecisionType.FUNCTION),
                new AuthorizationDecisionService.TokenVersions(3L, 6L, 7L));
    }

    private static CurrentRbac3ServicePrincipal servicePrincipal(String application) {
        return new CurrentRbac3ServicePrincipal(
                "tenant", "service", application, "prod", "default",
                "credential", Set.of("service:authorization:decide"));
    }

    private static RoleNode role(String id) {
        return new RoleNode(id, "application", id.toUpperCase(), true,
                RoleNode.RiskLevel.LOW, false, null, 100);
    }
}
