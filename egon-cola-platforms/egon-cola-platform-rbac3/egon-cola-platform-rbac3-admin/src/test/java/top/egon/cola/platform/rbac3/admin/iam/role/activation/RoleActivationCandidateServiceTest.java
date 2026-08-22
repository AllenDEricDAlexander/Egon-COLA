package top.egon.cola.platform.rbac3.admin.iam.role.activation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.repository.jpa.JpaRoleActivationFactRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleEligibilityService;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleActivationCandidateServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T00:00:00Z");

    @Test
    void activationFactRepositoryUsesAuthorizationStateForTenantPolicyVersion() {
        EntityManager entityManager = mock(EntityManager.class);
        TenantAuthorizationStateRepository stateStore = mock(
                TenantAuthorizationStateRepository.class);
        TenantAuthorizationStatePO state = new TenantAuthorizationStatePO(
                7L, "bootstrap", NOW);
        state.incrementPolicyVersion("operator", NOW.plusSeconds(1));
        when(stateStore.requireForUpdate(7L)).thenReturn(state);
        List<String> sqls = new ArrayList<>();
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            sqls.add(sql);
            Query query = mock(Query.class);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.getResultList()).thenReturn(
                    sql.contains("select u.auth_version")
                            ? Collections.singletonList(new Object[]{3L, 12L, "ACTIVE"})
                            : List.of());
            return query;
        });

        var facts = new JpaRoleActivationFactRepository(
                entityManager,
                mock(RoleEligibilityService.class),
                stateStore).load("7", "9", NOW);

        assertThat(facts.policyVersion()).isEqualTo(1L);
        assertThat(sqls).noneMatch(sql -> sql.contains("rbac3_tenant t"));
        verify(stateStore).requireForUpdate(7L);
    }

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

    @Test
    void omitsCandidatesWhenTheApplicationBusinessGrantIsNoLongerEffective() {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(role("10", "1", "FINANCE_ROOT", RoleNode.RiskLevel.MEDIUM)),
                List.of());
        var facts = new ActivationFactsVO(
                "7", "9", hierarchy,
                List.of(assignment("101", "9", "10")),
                List.of(),
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3, 8, "directory:12",
                Map.of("1", new ApplicationFactVO("1", "finance", "Finance")),
                Map.of("10", "Finance"));
        RoleEligibilityService eligibility = mock(RoleEligibilityService.class);
        when(eligibility.isEffective("7", "9", "1", NOW)).thenReturn(false);

        var result = new RoleActivationCandidateService(
                (tenantId, userId, databaseNow) -> facts, eligibility)
                .candidates("7", "9", NOW);

        assertThat(result.applications()).isEmpty();
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
