package top.egon.cola.platform.rbac3.admin.simulation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.admin.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO;

class AuthorizationSimulationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void evaluatesCurrentAndHypotheticalResultsFromOneSnapshotAndOnlyAppendsAudit() {
        AtomicInteger snapshotReads = new AtomicInteger();
        var snapshot = new SessionAuthorizationSnapshot(
                "session-1", 43, 2, 18,
                List.of(new AppAuthorizationContext(
                        "app-1", "finance-web", List.of("role-1"), List.of("assignment-1"),
                        List.of("role-1"), Set.of(), Map.of(), Map.of(), List.of(), null)),
                "sha256:simulation", NOW.minusSeconds(1));
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(
                (tenantId, sessionId) -> {
                    snapshotReads.incrementAndGet();
                    return new AuthorizationDecisionService.SnapshotRecord(
                            tenantId, "user-1", snapshot);
                },
                (tenantId, sessionId) -> false,
                Clock.fixed(NOW, ZoneOffset.UTC));
        List<AuditPort.AuditEvent> audits = new ArrayList<>();
        AuthorizationSimulationService service = new AuthorizationSimulationService(
                decisionService,
                (tenantId, roleId) -> new AuthorizationSimulationService.RoleImpactSnapshot(
                        new RoleImpactVO(
                                roleId, List.of(roleId), List.of(roleId),
                                "LOW", 0, List.of()),
                        18, "sha256:role-impact"),
                audits::add,
                Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new AuthorizationDecisionService.DecisionRequest(
                new AuthorizationDecisionService.Subject(
                        "tenant-1", "user-1", "session-1"),
                "finance:payment:approve",
                new AuthorizationDecisionService.Resource(
                        "finance-web", "payment-approvals"),
                EnumSet.of(AuthorizationDecisionService.DecisionType.FUNCTION),
                new AuthorizationDecisionService.TokenVersions(43, 2, 18));

        var result = service.simulate(
                principal(),
                new AuthorizationSimulationService.SimulationRequest(
                        request,
                        new AuthorizationSimulationService.Hypothesis(
                                Set.of("finance:payment:approve"), Set.of()),
                        NOW,
                        "simulation-request",
                        "simulation-trace"));

        assertThat(result.current().functionDecision().decision()).isEqualTo(Decision.DENY);
        assertThat(result.hypothetical().functionDecision().decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.authVersion()).isEqualTo(43);
        assertThat(result.sessionVersion()).isEqualTo(2);
        assertThat(result.policyVersion()).isEqualTo(18);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(snapshotReads).hasValue(1);
        assertThat(audits).singleElement().satisfies(audit -> {
            assertThat(audit.eventType()).isEqualTo("AUTHORIZATION_SIMULATED");
            assertThat(audit.safeEvidence()).doesNotContainKeys("token", "password", "secret");
        });
    }

    @Test
    void returnsVersionedRoleChangeImpactAndOnlyAppendsSimulationAudit() {
        List<AuditPort.AuditEvent> audits = new ArrayList<>();
        AuthorizationSimulationService service = new AuthorizationSimulationService(
                decisionServiceNotUsed(),
                (tenantId, roleId) -> new AuthorizationSimulationService.RoleImpactSnapshot(
                        new RoleImpactVO(
                                roleId, List.of("root-1"),
                                List.of("root-1", roleId), "HIGH", 7,
                                List.of("ROLE_ACTIVATION_ROOT_AMBIGUOUS")),
                        23, "sha256:impact-23"),
                audits::add,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.simulateRoleChangeImpact(
                principal(),
                new AuthorizationSimulationService.RoleChangeImpactRequest(
                        "role-7", NOW, "impact-request", "impact-trace"));

        assertThat(result.impact().roleId()).isEqualTo("role-7");
        assertThat(result.policyVersion()).isEqualTo(23);
        assertThat(result.evidenceChecksum()).isEqualTo("sha256:impact-23");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(audits).singleElement().satisfies(audit -> {
            assertThat(audit.eventType()).isEqualTo("ROLE_CHANGE_IMPACT_SIMULATED");
            assertThat(audit.targetType()).isEqualTo("ROLE");
            assertThat(audit.targetId()).isEqualTo("role-7");
        });
    }

    private AuthorizationDecisionService decisionServiceNotUsed() {
        return new AuthorizationDecisionService(
                (tenantId, sessionId) -> {
                    throw new AssertionError("authorization snapshot must not be read");
                },
                (tenantId, sessionId) -> false,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private CurrentRbac3Principal principal() {
        return new CurrentRbac3Principal(
                "tenant-1", "auditor-1", "admin-session", 8, 3, 18,
                Set.of("system:authorization-simulation:execute"), false);
    }
}
