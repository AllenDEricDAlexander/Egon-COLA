package top.egon.cola.platform.rbac3.core.hierarchy;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleHierarchyValidatorTest {

    @Test
    void acceptsDepthTenAndRejectsDepthEleven() {
        RoleHierarchy valid = chain(11);
        assertEquals(10, new RoleHierarchyValidator().validate(valid).maxDepth());

        Rbac3RuleViolation error = assertThrows(
                Rbac3RuleViolation.class,
                () -> new RoleHierarchyValidator().validate(chain(12))
        );
        assertEquals("ROLE_HIERARCHY_DEPTH_LIMIT_EXCEEDED", error.reasonCode());
    }

    @Test
    void rejectsCrossApplicationInheritanceAndCycles() {
        RoleHierarchy crossApplication = new RoleHierarchy(
                List.of(node("1", "a"), node("2", "b")),
                List.of(new RoleEdge("1", "2"))
        );
        assertEquals(
                "ROLE_HIERARCHY_CROSS_APPLICATION",
                assertThrows(Rbac3RuleViolation.class,
                        () -> new RoleHierarchyValidator().validate(crossApplication))
                        .reasonCode()
        );

        RoleHierarchy cycle = new RoleHierarchy(
                List.of(node("1", "a"), node("2", "a")),
                List.of(new RoleEdge("1", "2"), new RoleEdge("2", "1"))
        );
        assertEquals(
                "ROLE_HIERARCHY_CYCLE",
                assertThrows(Rbac3RuleViolation.class,
                        () -> new RoleHierarchyValidator().validate(cycle))
                        .reasonCode()
        );
    }

    private RoleHierarchy chain(int nodes) {
        var roleNodes = new java.util.ArrayList<RoleNode>();
        var edges = new java.util.ArrayList<RoleEdge>();
        for (int i = 0; i < nodes; i++) {
            roleNodes.add(node(Integer.toString(i), "app"));
            if (i > 0) {
                edges.add(new RoleEdge(Integer.toString(i - 1), Integer.toString(i)));
            }
        }
        return new RoleHierarchy(roleNodes, edges);
    }

    private RoleNode node(String id, String applicationId) {
        return new RoleNode(id, applicationId, "ROLE_" + id, true,
                RoleNode.RiskLevel.LOW, false, null, 1000);
    }
}
