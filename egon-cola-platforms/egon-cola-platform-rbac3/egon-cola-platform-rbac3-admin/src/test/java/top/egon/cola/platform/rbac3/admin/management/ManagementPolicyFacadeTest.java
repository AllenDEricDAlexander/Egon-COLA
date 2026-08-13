package top.egon.cola.platform.rbac3.admin.management;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.management.service.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicyRequestDTO;

class ManagementPolicyFacadeTest {

    @Test
    void neverCombinesFragmentsFromDifferentPolicies() {
        Instant now = Instant.parse("2026-07-30T08:00:00Z");
        var subjectOnly = policy("p1", Set.of("operator"), Set.of());
        var operationOnly = policy("p2", Set.of(), Set.of("ASSIGN_ROLE"));
        ManagementPolicyFacade facade = new ManagementPolicyFacade(
                new ManagementPolicyDecisionService(),
                (tenantId, subjectId, targetUserId, databaseNow) ->
                        List.of(subjectOnly, operationOnly));

        assertThatThrownBy(() -> facade.authorize(new ManagementPolicyRequestDTO(
                "10001", "operator", "target", "root", "ASSIGN_ROLE",
                "MFA", "MEDIUM", 3, true, true, now)))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("MANAGEMENT_OPERATION_DENIED");
    }

    private ManagementPolicyDecisionService.ManagementPolicyFact policy(
            String id,
            Set<String> subjects,
            Set<String> operations
    ) {
        return new ManagementPolicyDecisionService.ManagementPolicyFact(
                id, subjects, Set.of("target"), Set.of("root"), operations,
                new ManagementPolicyDecisionService.Restrictions(
                        30, "HIGH", "PASSWORD", true, false),
                Instant.parse("2026-01-01T00:00:00Z"), null, true);
    }
}
