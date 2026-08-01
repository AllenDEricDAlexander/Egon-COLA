package top.egon.cola.platform.rbac3.core.assignment;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentEligibilitySpecificationTest {

    private static final Instant NOW = Instant.parse("2026-07-30T10:00:00Z");

    @Test
    void usesHalfOpenValidityAndRequiresActiveStatus() {
        AssignmentEligibilitySpecification specification =
                new AssignmentEligibilitySpecification();
        assertTrue(specification.evaluate(fact(
                EligibleAssignmentFact.Status.ACTIVE,
                NOW.minusSeconds(1), NOW.plusSeconds(1)), NOW).allowed());
        assertFalse(specification.evaluate(fact(
                EligibleAssignmentFact.Status.ACTIVE,
                NOW.minusSeconds(1), NOW), NOW).allowed());
        assertFalse(specification.evaluate(fact(
                EligibleAssignmentFact.Status.SUSPENDED,
                NOW.minusSeconds(1), NOW.plusSeconds(1)), NOW).allowed());
    }

    private EligibleAssignmentFact fact(
            EligibleAssignmentFact.Status status,
            Instant from,
            Instant to
    ) {
        return new EligibleAssignmentFact("a1", "u1", "r1", status, from, to);
    }
}
