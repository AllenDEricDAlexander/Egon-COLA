package top.egon.cola.platform.rbac3.core.participation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationSodSpecificationTest {

    @Test
    void participationConflictSurvivesSessionAndRoleSwitching() {
        OperationSodSpecification specification = new OperationSodSpecification();
        var prior = new OperationSodSpecification.ParticipationFact(
                "event-1", "payment", "p-1", "user-1", "CREATE");
        assertFalse(specification.evaluate(
                "payment", "p-1", "user-1", "APPROVE",
                List.of(prior), MapRules.createApprove()).allowed());
        assertTrue(specification.evaluate(
                "payment", "p-2", "user-1", "APPROVE",
                List.of(prior), MapRules.createApprove()).allowed());
    }

    private static final class MapRules {
        private static java.util.Map<String, Set<String>> createApprove() {
            return java.util.Map.of("APPROVE", Set.of("CREATE"));
        }
    }
}
