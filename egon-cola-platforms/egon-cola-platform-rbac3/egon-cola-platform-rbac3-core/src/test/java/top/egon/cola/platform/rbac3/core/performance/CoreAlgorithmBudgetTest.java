package top.egon.cola.platform.rbac3.core.performance;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreAlgorithmBudgetTest {

    private static final int ROLE_COUNT = 1_000;

    @Test
    void wideHierarchyVisitsEachRoleOnceWithinTheOperationBudget() {
        RoleHierarchy hierarchy = wideHierarchy();

        new RoleHierarchyValidator().validate(hierarchy);
        var descendants = hierarchy.descendantsIncludingSelf("root");

        assertEquals(ROLE_COUNT, descendants.size());
        assertTrue(hierarchy.edges().size() <= ROLE_COUNT - 1);
        assertEquals(ROLE_COUNT, descendants.stream().distinct().count());
    }

    @Test
    void calibratedEnvironmentMayEnforceWallClockBudget() {
        Assumptions.assumeTrue(Boolean.getBoolean("rbac3.performance.enforce"));
        RoleHierarchy hierarchy = wideHierarchy();

        Instant started = Instant.now();
        for (int index = 0; index < 100; index++) {
            new RoleHierarchyValidator().validate(hierarchy);
            hierarchy.descendantsIncludingSelf("root");
        }

        assertTrue(Duration.between(started, Instant.now()).compareTo(
                Duration.ofSeconds(2)) < 0);
    }

    private static RoleHierarchy wideHierarchy() {
        List<RoleNode> nodes = new ArrayList<>();
        List<RoleEdge> edges = new ArrayList<>();
        nodes.add(node("root"));
        for (int index = 1; index < ROLE_COUNT; index++) {
            String id = "role-" + index;
            nodes.add(node(id));
            edges.add(new RoleEdge("root", id));
        }
        return new RoleHierarchy(nodes, edges);
    }

    private static RoleNode node(String id) {
        return new RoleNode(id, "application", id.toUpperCase(), true,
                RoleNode.RiskLevel.LOW, false, null, ROLE_COUNT);
    }
}
