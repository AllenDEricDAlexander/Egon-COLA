package top.egon.cola.platform.rbac3.core.constraint;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rbac3ConstraintSpecificationTest {

    @Test
    void evaluatesSsdPrerequisiteCapacityAndSelfAssignment() {
        assertFalse(new SsdSpecification().evaluate(
                Set.of("cashier", "approver"),
                List.of(new SsdSpecification.SsdSet("ssd-1", 1,
                        Set.of("cashier", "approver")))).allowed());
        assertTrue(new PrerequisiteRoleSpecification().evaluate(
                Set.of("base", "audit"),
                new PrerequisiteRoleSpecification.PrerequisiteGroup(
                        "g1", PrerequisiteRoleSpecification.MatchMode.ALL_OF,
                        Set.of("base", "audit"))).allowed());
        assertFalse(new RoleCardinalitySpecification().evaluate(10, 10).allowed());
        assertFalse(new SelfAssignmentSpecification().evaluate(
                "operator", "operator", "ASSIGN_ROLE").allowed());
        assertTrue(new SelfAssignmentSpecification().evaluate(
                "operator", "operator", "ACTIVATE_ROLE").allowed());
    }
}
