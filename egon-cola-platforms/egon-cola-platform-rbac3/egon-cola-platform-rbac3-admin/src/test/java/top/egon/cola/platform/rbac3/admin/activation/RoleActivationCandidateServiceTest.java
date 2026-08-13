package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ApplicationFactVO;

class RoleActivationCandidateServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T00:00:00Z");

    @Test
    void deduplicatesAssignmentsIntoOneCanonicalRootCandidate() {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(
                        role("10", "1", "FINANCE_ROOT", RoleNode.RiskLevel.MEDIUM),
                        role("11", "1", "FINANCE_AP", RoleNode.RiskLevel.HIGH)),
                List.of(new RoleEdge("10", "11")));
        var facts = new ActivationFactsVO(
                "7",
                "9",
                hierarchy,
                List.of(
                        assignment("101", "9", "10"),
                        assignment("102", "9", "11")),
                List.of(new DsdSetFact("501", "1", 1, Set.of("10"))),
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3,
                8,
                "directory:12",
                Map.of("1", new ApplicationFactVO(
                        "1", "finance", "Finance")),
                Map.of("10", "Finance", "11", "Accounts payable"));
        RoleActivationCandidateService service = new RoleActivationCandidateService(
                (tenantId, userId, databaseNow) -> facts);

        var result = service.candidates("7", "9", NOW);

        assertThat(result.applications()).hasSize(1);
        assertThat(result.applications().getFirst().applicationCode()).isEqualTo("finance");
        assertThat(result.applications().getFirst().candidates()).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.rootRoleId()).isEqualTo("10");
                    assertThat(candidate.sourceRoleIds()).containsExactly("10", "11");
                    assertThat(candidate.eligibleAssignmentIds()).containsExactly("101", "102");
                    assertThat(candidate.mutexSetIds()).containsExactly("501");
                    assertThat(candidate.effectiveFamilyRisk()).isEqualTo("HIGH");
                    assertThat(candidate.requiredAuthStrength()).isEqualTo("MFA");
                });
        assertThat(result.basedOnAuthVersion()).isEqualTo(3);
        assertThat(result.basedOnPolicyVersion()).isEqualTo(8);
        assertThat(result.basedOnDirectorySnapshotVersion()).isEqualTo("directory:12");
    }

    private static RoleNode role(
            String id,
            String applicationId,
            String code,
            RoleNode.RiskLevel risk
    ) {
        return new RoleNode(id, applicationId, code, true, risk,
                false, null, 100);
    }

    private static EligibleAssignmentFact assignment(
            String id,
            String userId,
            String roleId
    ) {
        return new EligibleAssignmentFact(
                id, userId, roleId, EligibleAssignmentFact.Status.ACTIVE,
                NOW.minusSeconds(60), null);
    }
}
