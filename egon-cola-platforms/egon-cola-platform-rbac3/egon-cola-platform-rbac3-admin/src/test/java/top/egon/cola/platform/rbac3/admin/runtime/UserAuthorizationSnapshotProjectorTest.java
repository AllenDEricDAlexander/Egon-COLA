package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.rbac3.admin.iam.role.service.EffectiveApplicationScope;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleEligibilityService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.ProjectionCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.service.UserAuthorizationSnapshotProjector;
import top.egon.cola.platform.rbac3.core.activation.ActivationAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.ActiveRoleSet;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAuthorizationSnapshotProjectorTest {

    private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

    @Test
    void doesNotProjectAnApplicationContextAfterItsBusinessGrantIsRevoked() {
        RoleNode role = new RoleNode(
                "10", "1", "FINANCE_ROOT", true,
                RoleNode.RiskLevel.MEDIUM, false, null, 100);
        ActivationFactsVO facts = new ActivationFactsVO(
                "7", "9", new RoleHierarchy(List.of(role), List.of()),
                List.of(new EligibleAssignmentFact(
                        "101", "9", "10", EligibleAssignmentFact.Status.ACTIVE,
                        NOW.minusSeconds(60), null)),
                List.of(),
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3, 8, "directory:12",
                Map.of("1", new ApplicationFactVO("1", "finance", "Finance")),
                Map.of("10", "Finance"));
        RoleActivationResolution resolution = new RoleActivationResolution(
                new ActiveRoleSet("7", "9", Map.of("1", Set.of("10")), "active"),
                new ActivationAuthorizationSnapshot(
                        Set.of("10"), Set.of(), Map.of(), Map.of(), Set.of(),
                        null, 3, 8, "snapshot"),
                List.of("101"));
        RoleEligibilityService eligibility = mock(RoleEligibilityService.class);
        when(eligibility.resolveEffectiveScope("7", "9", "1", NOW))
                .thenReturn(Optional.empty());

        var projection = new UserAuthorizationSnapshotProjector(eligibility).project(
                new ProjectionCommandDTO(
                        "7", "idp-9", "9", 3, 8,
                        NOW.plusSeconds(3600), resolution, facts, NOW));

        assertThat(projection.snapshot().appContexts()).isEmpty();
        assertThat(projection.gatewayScope().businesses()).isEmpty();
    }

    @Test
    void projectsGatewayScopeFromEffectiveActiveApplicationContexts() {
        RoleEligibilityService eligibility = mock(RoleEligibilityService.class);
        when(eligibility.resolveEffectiveScope("7", "9", "1", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "ddc-biz-2", "sales", "ddc-app-2", "portal")));
        when(eligibility.resolveEffectiveScope("7", "9", "2", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "ddc-biz-1", "finance", "ddc-app-1", "console")));
        when(eligibility.resolveEffectiveScope("7", "9", "3", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "ddc-biz-1", "finance", "ddc-app-3", "analytics")));

        var projection = new UserAuthorizationSnapshotProjector(eligibility)
                .project(commandWithActiveApplications());

        assertThat(projection.gatewayScope().businesses())
                .extracting(scope -> scope.businessCode())
                .containsExactly("finance", "sales");
        assertThat(projection.gatewayScope().businesses().getFirst().applications())
                .extracting(scope -> scope.applicationCode())
                .containsExactly("analytics", "console");
        assertThat(projection.gatewayScope().businesses().getLast().applications())
                .extracting(scope -> scope.applicationCode())
                .containsExactly("portal");
        assertThat(projection.gatewayScope().tenantId())
                .isEqualTo(projection.snapshot().tenantId());
        assertThat(projection.gatewayScope().identitySub())
                .isEqualTo(projection.snapshot().identitySub());
        assertThat(projection.gatewayScope().rbacUserId())
                .isEqualTo(projection.snapshot().rbacUserId());
        assertThat(projection.gatewayScope().authVersion())
                .isEqualTo(projection.snapshot().authVersion());
        assertThat(projection.gatewayScope().policyVersion())
                .isEqualTo(projection.snapshot().policyVersion());
        assertThat(projection.gatewayScope().generatedAt())
                .isEqualTo(projection.snapshot().generatedAt());
        assertThat(projection.gatewayScope().expiresAt())
                .isEqualTo(projection.snapshot().expiresAt());
        assertThat(projection.gatewayScope().checksum()).isNotBlank();
    }

    @Test
    void keepsFullSnapshotPermissionsUnchanged() {
        RoleEligibilityService eligibility = mock(RoleEligibilityService.class);
        when(eligibility.resolveEffectiveScope("7", "9", "1", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "ddc-biz-2", "sales", "ddc-app-2", "portal")));
        when(eligibility.resolveEffectiveScope("7", "9", "2", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "ddc-biz-1", "finance", "ddc-app-1", "console")));
        when(eligibility.resolveEffectiveScope("7", "9", "3", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "ddc-biz-1", "finance", "ddc-app-3", "analytics")));

        var first = new UserAuthorizationSnapshotProjector(eligibility)
                .project(commandWithActiveApplications());
        var second = new UserAuthorizationSnapshotProjector(eligibility)
                .project(commandWithActiveApplications());

        assertThat(first.snapshot().appContexts())
                .extracting(context -> context.permissions())
                .containsExactly(
                        Set.of("finance:read"),
                        Set.of("finance:report"),
                        Set.of("sales:read"));
        assertThat(first.snapshot().checksum()).isEqualTo("full-snapshot-checksum");
        assertThat(first.gatewayScope().checksum())
                .isEqualTo(second.gatewayScope().checksum())
                .isNotEqualTo(first.snapshot().checksum());
    }

    private ProjectionCommandDTO commandWithActiveApplications() {
        RoleNode salesRole = new RoleNode(
                "10", "1", "SALES_ROOT", true,
                RoleNode.RiskLevel.MEDIUM, false, null, 100);
        RoleNode financeRole = new RoleNode(
                "20", "2", "FINANCE_ROOT", true,
                RoleNode.RiskLevel.MEDIUM, false, null, 100);
        RoleNode reportRole = new RoleNode(
                "30", "3", "FINANCE_REPORT_ROOT", true,
                RoleNode.RiskLevel.MEDIUM, false, null, 100);
        ActivationFactsVO facts = new ActivationFactsVO(
                "7", "9", new RoleHierarchy(
                        List.of(salesRole, financeRole, reportRole), List.of()),
                List.of(
                        new EligibleAssignmentFact(
                                "101", "9", "10", EligibleAssignmentFact.Status.ACTIVE,
                                NOW.minusSeconds(60), null),
                        new EligibleAssignmentFact(
                                "102", "9", "20", EligibleAssignmentFact.Status.ACTIVE,
                                NOW.minusSeconds(60), null),
                        new EligibleAssignmentFact(
                                "103", "9", "30", EligibleAssignmentFact.Status.ACTIVE,
                                NOW.minusSeconds(60), null)),
                List.of(),
                new AuthorizationRuleFacts(
                        List.of(
                                new AuthorizationRuleFacts.PermissionBinding(
                                        "10", "sales:read"),
                                new AuthorizationRuleFacts.PermissionBinding(
                                        "20", "finance:read"),
                                new AuthorizationRuleFacts.PermissionBinding(
                                        "30", "finance:report")),
                        List.of(), List.of(), List.of(), List.of()),
                3, 8, "directory:12",
                Map.of(
                        "1", new ApplicationFactVO("1", "sales-app", "Sales"),
                        "2", new ApplicationFactVO("2", "finance-app", "Finance"),
                        "3", new ApplicationFactVO("3", "reports-app", "Reports")),
                Map.of("10", "Sales", "20", "Finance", "30", "Reports"));
        RoleActivationResolution resolution = new RoleActivationResolution(
                new ActiveRoleSet(
                        "7", "9", Map.of(
                                "1", Set.of("10"),
                                "2", Set.of("20"),
                                "3", Set.of("30")),
                        "active"),
                new ActivationAuthorizationSnapshot(
                        Set.of("10", "20", "30"),
                        Set.of("sales:read", "finance:read", "finance:report"),
                        Map.of(), Map.of(), Set.of(), null,
                        3, 8, "full-snapshot-checksum"),
                List.of("101", "102", "103"));
        return new ProjectionCommandDTO(
                "7", "idp-9", "9", 3, 8,
                NOW.plusSeconds(3600), resolution, facts, NOW);
    }
}
