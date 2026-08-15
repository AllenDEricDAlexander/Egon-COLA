package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ApplicationFactVO;
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
        when(eligibility.isEffective("7", "9", "1", NOW)).thenReturn(false);

        var projection = new UserAuthorizationSnapshotProjector(eligibility).project(
                new ProjectionCommandDTO(
                        "7", "idp-9", "9", 3, 8,
                        NOW.plusSeconds(3600), resolution, facts, NOW));

        assertThat(projection.snapshot().appContexts()).isEmpty();
    }
}
