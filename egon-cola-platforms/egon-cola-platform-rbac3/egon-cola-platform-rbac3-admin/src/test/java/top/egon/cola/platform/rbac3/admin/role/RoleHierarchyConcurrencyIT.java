package top.egon.cola.platform.rbac3.admin.role;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleHierarchyConcurrencyIT {

    @Test
    void serializesGraphMutationAndRejectsCycleBeforeClosureRebuild() {
        var store = new InMemoryHierarchyStore();
        var facade = new RoleFacade(store);

        facade.addInheritance("tenant", "app", "root", "child");
        Rbac3RuleViolation cycle = assertThrows(Rbac3RuleViolation.class,
                () -> facade.addInheritance("tenant", "app", "child", "root"));

        assertEquals("ROLE_HIERARCHY_CYCLE", cycle.reasonCode());
        assertEquals(List.of(new RoleEdge("root", "child")), store.edges);
        assertEquals(1, store.rebuildCount);
    }

    private static final class InMemoryHierarchyStore implements RoleFacade.HierarchyStore {
        private final List<RoleNode> nodes = List.of(
                node("root"), node("child"));
        private final List<RoleEdge> edges = new ArrayList<>();
        private int rebuildCount;

        @Override
        public <T> T withGraphLock(
                String tenantId,
                String applicationId,
                java.util.function.Function<RoleHierarchy, T> action) {
            return action.apply(new RoleHierarchy(nodes, edges));
        }

        @Override
        public void addEdge(String tenantId, String applicationId, RoleEdge edge) {
            edges.add(edge);
        }

        @Override
        public void removeEdge(String tenantId, String applicationId, RoleEdge edge) {
            edges.remove(edge);
        }

        @Override
        public void rebuildClosure(String tenantId, String applicationId) {
            rebuildCount++;
        }

        private RoleNode node(String id) {
            return new RoleNode(id, "app", id.toUpperCase(), true,
                    RoleNode.RiskLevel.LOW, false, null, 1000);
        }
    }
}
