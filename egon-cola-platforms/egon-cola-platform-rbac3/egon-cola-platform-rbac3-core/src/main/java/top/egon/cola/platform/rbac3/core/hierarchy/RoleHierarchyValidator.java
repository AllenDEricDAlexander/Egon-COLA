package top.egon.cola.platform.rbac3.core.hierarchy;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.HashMap;
import java.util.Map;

public final class RoleHierarchyValidator {

    public static final int MAX_DEPTH = 10;

    public ValidationResult validate(RoleHierarchy hierarchy) {
        for (RoleEdge edge : hierarchy.edges()) {
            RoleNode senior = hierarchy.requireNode(edge.seniorRoleId());
            RoleNode junior = hierarchy.requireNode(edge.juniorRoleId());
            if (!senior.applicationId().equals(junior.applicationId())) {
                throw new Rbac3RuleViolation(
                        "ROLE_HIERARCHY_CROSS_APPLICATION",
                        java.util.List.of(senior.id(), junior.id())
                );
            }
        }
        Map<String, VisitState> state = new HashMap<>();
        Map<String, Integer> depthMemo = new HashMap<>();
        int maxDepth = 0;
        for (String roleId : hierarchy.nodes().keySet()) {
            maxDepth = Math.max(maxDepth, depth(roleId, hierarchy, state, depthMemo));
        }
        if (maxDepth > MAX_DEPTH) {
            throw new Rbac3RuleViolation("ROLE_HIERARCHY_DEPTH_LIMIT_EXCEEDED");
        }
        return new ValidationResult(maxDepth, hierarchy.nodes().size(), hierarchy.edges().size());
    }

    private int depth(
            String roleId,
            RoleHierarchy hierarchy,
            Map<String, VisitState> state,
            Map<String, Integer> memo
    ) {
        Integer cached = memo.get(roleId);
        if (cached != null) {
            return cached;
        }
        if (state.get(roleId) == VisitState.VISITING) {
            throw new Rbac3RuleViolation("ROLE_HIERARCHY_CYCLE", java.util.List.of(roleId));
        }
        state.put(roleId, VisitState.VISITING);
        int result = 0;
        for (String child : hierarchy.childrenOf(roleId)) {
            result = Math.max(result, 1 + depth(child, hierarchy, state, memo));
        }
        state.put(roleId, VisitState.VISITED);
        memo.put(roleId, result);
        return result;
    }

    public record ValidationResult(int maxDepth, int nodeCount, int edgeCount) {
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
