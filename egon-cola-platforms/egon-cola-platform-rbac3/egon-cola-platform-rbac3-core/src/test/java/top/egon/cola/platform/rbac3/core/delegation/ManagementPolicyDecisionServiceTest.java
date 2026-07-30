package top.egon.cola.platform.rbac3.core.delegation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagementPolicyDecisionServiceTest {

    @Test
    void onePolicyMustAuthorizeEveryDimension() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        var subjectOnly = policy("p1", Set.of("manager"), Set.of("other-user"),
                Set.of("report-root"), Set.of("ASSIGN_ROLE"), now);
        var targetOnly = policy("p2", Set.of("other-manager"), Set.of("user"),
                Set.of("payment-root"), Set.of("ASSIGN_ROLE"), now);
        var input = new ManagementPolicyDecisionService.ManagementDecisionInput(
                "manager", "user", "payment-root", "ASSIGN_ROLE",
                "MFA", "LOW", 7, true, true, now,
                List.of(subjectOnly, targetOnly));

        assertFalse(new ManagementPolicyDecisionService().decide(input).allowed());

        var complete = policy("p3", Set.of("manager"), Set.of("user"),
                Set.of("payment-root"), Set.of("ASSIGN_ROLE"), now);
        assertTrue(new ManagementPolicyDecisionService().decide(
                new ManagementPolicyDecisionService.ManagementDecisionInput(
                        "manager", "user", "payment-root", "ASSIGN_ROLE",
                        "MFA", "LOW", 7, true, true, now,
                        List.of(subjectOnly, targetOnly, complete))).allowed());
    }

    private ManagementPolicyDecisionService.ManagementPolicyFact policy(
            String id,
            Set<String> subjects,
            Set<String> targets,
            Set<String> roots,
            Set<String> operations,
            Instant now
    ) {
        return new ManagementPolicyDecisionService.ManagementPolicyFact(
                id, subjects, targets, roots, operations,
                new ManagementPolicyDecisionService.Restrictions(
                        30, "MEDIUM", "MFA", true, true),
                now.minusSeconds(60), now.plusSeconds(60), true);
    }
}
