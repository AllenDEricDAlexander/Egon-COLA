package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.service.EffectiveApplicationScope;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.service.RoleEligibilityService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.ProjectionCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.UserAuthorizationSnapshotProjector;
import top.egon.cola.platform.tianquan.jianshen.core.activation.ActivationAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.tianquan.jianshen.core.activation.ActiveRoleSet;
import top.egon.cola.platform.tianquan.jianshen.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.tianquan.jianshen.core.activation.RoleActivationResolution;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.tianquan.jianshen.core.decision.UserAuthorizationSnapshotBuilder;
import top.egon.cola.platform.tianquan.jianshen.core.hierarchy.RoleEdge;
import top.egon.cola.platform.tianquan.jianshen.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.tianquan.jianshen.core.hierarchy.RoleNode;

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
                        "7", "tianquan-shoubing-9", "9", 3, 8,
                        NOW.plusSeconds(3600), resolution, facts, NOW));

        assertThat(projection.snapshot().appContexts()).isEmpty();
        assertThat(projection.gatewayScope().businesses()).isEmpty();
    }

    @Test
    void projectsGatewayScopeFromEffectiveActiveApplicationContexts() {
        RoleEligibilityService eligibility = mock(RoleEligibilityService.class);
        when(eligibility.resolveEffectiveScope("7", "9", "1", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "tianshu-biz-2", "sales", "tianshu-app-2", "portal")));
        when(eligibility.resolveEffectiveScope("7", "9", "2", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "tianshu-biz-1", "finance", "tianshu-app-1", "console")));
        when(eligibility.resolveEffectiveScope("7", "9", "3", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "tianshu-biz-1", "finance", "tianshu-app-3", "analytics")));

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
                        "tianshu-biz-2", "sales", "tianshu-app-2", "portal")));
        when(eligibility.resolveEffectiveScope("7", "9", "2", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "tianshu-biz-1", "finance", "tianshu-app-1", "console")));
        when(eligibility.resolveEffectiveScope("7", "9", "3", NOW))
                .thenReturn(Optional.of(new EffectiveApplicationScope(
                        "tianshu-biz-1", "finance", "tianshu-app-3", "analytics")));

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

    @Test
    void preservesPublicConstructorsAndQualifiedSpringInjection() throws NoSuchMethodException {
        var constructor = UserAuthorizationSnapshotProjector.class
                .getConstructor(RoleEligibilityService.class);

        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
        assertThat(constructor.getParameters()[0].getAnnotation(Qualifier.class).value())
                .isEqualTo("roleEligibilityService");
        assertThat(UserAuthorizationSnapshotProjector.class.getConstructor()).isNotNull();
    }

    @Test
    void projectsFieldPoliciesOnlyForResourcesGrantedInTheCurrentApplication() {
        ProjectionCommandDTO command = commandWithFieldPolicies(false);
        var projection = new UserAuthorizationSnapshotProjector().project(command);
        var appA = projection.snapshot().appContexts().getFirst();
        var appB = projection.snapshot().appContexts().getLast();

        assertThat(appA.applicationCode()).isEqualTo("app-a");
        assertThat(appA.fieldPolicies()).containsOnlyKeys("permission-a:app-a:resource-a");
        assertThat(appA.fieldPolicies().get("permission-a:app-a:resource-a")
                .fields().get("name").level()).isEqualTo(FieldAccessLevel.READ);
        assertThat(appB.applicationCode()).isEqualTo("app-b");
        assertThat(appB.fieldPolicies()).containsOnlyKeys("permission-b:app-b:resource-b");
        assertThat(appB.fieldPolicies().get("permission-b:app-b:resource-b")
                .fields().get("secret").level()).isEqualTo(FieldAccessLevel.WRITE);
        assertThat(projection.snapshot().checksum()).isEqualTo(command.resolution().snapshot().checksum());
    }

    @Test
    void projectsFieldPoliciesOnlyForTheGrantedPermissionResourceTupleWithinAnApplication() {
        ProjectionCommandDTO command = commandWithFieldPolicies(true);
        var app = new UserAuthorizationSnapshotProjector().project(command)
                .snapshot().appContexts().getFirst();

        assertThat(app.fieldPolicies()).containsOnlyKeys(
                "permission-a:app-a:resource-a", "permission-b:app-a:resource-b");
        assertThat(app.fieldPolicies().get("permission-a:app-a:resource-a")
                .fields().get("name").level()).isEqualTo(FieldAccessLevel.READ);
        assertThat(app.fieldPolicies().get("permission-b:app-a:resource-b")
                .fields().get("secret").level()).isEqualTo(FieldAccessLevel.WRITE);
        assertThat(app.permissions()).containsExactlyInAnyOrder("permission-a", "permission-b");
        assertThat(app.activationRootRoleIds()).containsExactly("10");
        assertThat(app.effectiveRoleIds()).containsExactly("10", "20");
    }

    private ProjectionCommandDTO commandWithFieldPolicies(boolean sameApplication) {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(
                        new RoleNode("10", "1", "APP_A_ROOT", true,
                                RoleNode.RiskLevel.LOW, false, null, 100),
                        new RoleNode("20", sameApplication ? "1" : "2", "APP_B_ROOT",
                                !sameApplication, RoleNode.RiskLevel.LOW, false, null, 100)),
                sameApplication ? List.of(new RoleEdge("10", "20")) : List.of());
        AuthorizationRuleFacts rules = new AuthorizationRuleFacts(
                List.of(
                        new AuthorizationRuleFacts.ResourceGrantBinding(
                                "10", "101", "resource-a", "API", "permission-a"),
                        new AuthorizationRuleFacts.ResourceGrantBinding(
                                "20", "201", "resource-b", "API", "permission-b")),
                List.of(),
                List.of(
                        new AuthorizationRuleFacts.FieldRuleFact(
                                "10", "resource-a", "name", FieldAccessLevel.READ),
                        new AuthorizationRuleFacts.FieldRuleFact(
                                "20", "resource-b", "secret", FieldAccessLevel.WRITE)),
                List.of(
                        new AuthorizationRuleFacts.FieldDefinitionFact(
                                "resource-a", "name", FieldAccessLevel.WRITE),
                        new AuthorizationRuleFacts.FieldDefinitionFact(
                                "resource-b", "secret", FieldAccessLevel.WRITE)),
                List.of(
                        new AuthorizationRuleFacts.ResourceFact(
                                "101", "resource-a", "API", null, "permission-a"),
                        new AuthorizationRuleFacts.ResourceFact(
                                "201", "resource-b", "API", null, "permission-b")));
        ActivationFactsVO facts = new ActivationFactsVO(
                "7", "9", hierarchy,
                List.of(
                        new EligibleAssignmentFact("101", "9", "10",
                                EligibleAssignmentFact.Status.ACTIVE, NOW.minusSeconds(60), null),
                        new EligibleAssignmentFact("102", "9", "20",
                                EligibleAssignmentFact.Status.ACTIVE, NOW.minusSeconds(60), null)),
                List.of(), rules, 3, 8, "directory:12",
                Map.of(
                        "1", new ApplicationFactVO("1", "app-a", "App A"),
                        "2", new ApplicationFactVO("2", "app-b", "App B")),
                Map.of("10", "App A", "20", "App B"));
        var snapshot = new UserAuthorizationSnapshotBuilder().build(
                sameApplication ? Set.of("10") : Set.of("10", "20"),
                hierarchy, rules, 3, 3, 8);
        RoleActivationResolution resolution = new RoleActivationResolution(
                new ActiveRoleSet("7", "9", sameApplication
                        ? Map.of("1", Set.of("10"))
                        : Map.of("1", Set.of("10"), "2", Set.of("20")), "active"),
                snapshot, List.of("101", "102"));
        return new ProjectionCommandDTO(
                "7", "tianquan-shoubing-9", "9", 3, 8,
                NOW.plusSeconds(3600), resolution, facts, NOW);
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
                                new AuthorizationRuleFacts.ResourceGrantBinding(
                                        "10", "sales:read"),
                                new AuthorizationRuleFacts.ResourceGrantBinding(
                                        "20", "finance:read"),
                                new AuthorizationRuleFacts.ResourceGrantBinding(
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
                "7", "tianquan-shoubing-9", "9", 3, 8,
                NOW.plusSeconds(3600), resolution, facts, NOW);
    }
}
