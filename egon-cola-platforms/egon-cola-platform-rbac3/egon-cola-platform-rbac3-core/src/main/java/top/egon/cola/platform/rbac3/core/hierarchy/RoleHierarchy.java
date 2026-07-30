package top.egon.cola.platform.rbac3.core.hierarchy;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class RoleHierarchy {

    private final Map<String, RoleNode> nodes;
    private final List<RoleEdge> edges;
    private final Map<String, Set<String>> parents;
    private final Map<String, Set<String>> children;

    public RoleHierarchy(Collection<RoleNode> nodes, Collection<RoleEdge> edges) {
        var indexed = new LinkedHashMap<String, RoleNode>();
        for (RoleNode node : List.copyOf(nodes)) {
            if (indexed.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("duplicate role node: " + node.id());
            }
        }
        this.nodes = Collections.unmodifiableMap(indexed);
        this.edges = List.copyOf(edges);
        this.parents = adjacency(this.edges, false);
        this.children = adjacency(this.edges, true);
    }

    public Map<String, RoleNode> nodes() {
        return nodes;
    }

    public List<RoleEdge> edges() {
        return edges;
    }

    public RoleNode requireNode(String roleId) {
        RoleNode node = nodes.get(roleId);
        if (node == null) {
            throw new IllegalArgumentException("unknown role: " + roleId);
        }
        return node;
    }

    public Set<String> parentsOf(String roleId) {
        return parents.getOrDefault(roleId, Set.of());
    }

    public Set<String> childrenOf(String roleId) {
        return children.getOrDefault(roleId, Set.of());
    }

    public Set<String> rootsOf(String roleId) {
        requireNode(roleId);
        var roots = new TreeSet<String>();
        var visited = new LinkedHashSet<String>();
        var queue = new ArrayDeque<String>();
        queue.add(roleId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            Set<String> currentParents = parentsOf(current);
            if (currentParents.isEmpty()) {
                roots.add(current);
            } else {
                queue.addAll(currentParents);
            }
        }
        return Collections.unmodifiableSet(roots);
    }

    public Set<String> descendantsIncludingSelf(String rootRoleId) {
        requireNode(rootRoleId);
        var descendants = new TreeSet<String>();
        var queue = new ArrayDeque<String>();
        queue.add(rootRoleId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (descendants.add(current)) {
                queue.addAll(childrenOf(current));
            }
        }
        return Collections.unmodifiableSet(descendants);
    }

    private Map<String, Set<String>> adjacency(List<RoleEdge> values, boolean forward) {
        var result = new LinkedHashMap<String, Set<String>>();
        for (RoleEdge edge : values) {
            String key = forward ? edge.seniorRoleId() : edge.juniorRoleId();
            String value = forward ? edge.juniorRoleId() : edge.seniorRoleId();
            result.computeIfAbsent(key, ignored -> new TreeSet<>()).add(value);
        }
        result.replaceAll((ignored, value) -> Collections.unmodifiableSet(value));
        return Collections.unmodifiableMap(result);
    }
}
