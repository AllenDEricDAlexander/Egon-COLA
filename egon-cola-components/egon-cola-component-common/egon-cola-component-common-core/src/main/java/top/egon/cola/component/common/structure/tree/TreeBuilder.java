package top.egon.cola.component.common.structure.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder for parent-child tree structures.
 */
public final class TreeBuilder {

    private TreeBuilder() {
    }

    public static <ID, V> List<TreeNode<ID, V>> build(List<TreeNode<ID, V>> nodes) {
        return build(nodes, new TreeOptions());
    }

    public static <ID, V> List<TreeNode<ID, V>> build(List<TreeNode<ID, V>> nodes, TreeOptions options) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        Map<ID, TreeNode<ID, V>> nodeMap = new LinkedHashMap<>();
        for (TreeNode<ID, V> node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        Set<ID> cycleBreaks = findCycleBreaks(nodeMap);
        if (!cycleBreaks.isEmpty() && options.isFailOnCycle()) {
            throw new IllegalArgumentException("tree input contains a parent cycle at node ids " + cycleBreaks);
        }

        List<TreeNode<ID, V>> roots = new ArrayList<>();
        for (TreeNode<ID, V> node : nodes) {
            ID parentId = node.getParentId();
            TreeNode<ID, V> parent = parentId == null ? null : nodeMap.get(parentId);
            if (parent == null) {
                if (parentId == null || options.isKeepOrphansAsRoots()) {
                    roots.add(node);
                }
                continue;
            }
            if (cycleBreaks.contains(node.getId())) {
                roots.add(node);
                continue;
            }
            parent.addChild(node);
        }
        return roots;
    }

    /**
     * Returns one node id per parent cycle, chosen as the point at which the cycle closes.
     *
     * <p>Promoting exactly these ids to roots keeps every node reachable: the remaining members of
     * each cycle still attach to their parents, so only the single closing edge is dropped. A node
     * whose parent id equals its own id is the degenerate one-element case.
     */
    private static <ID, V> Set<ID> findCycleBreaks(Map<ID, TreeNode<ID, V>> nodeMap) {
        Set<ID> cycleBreaks = new HashSet<>();
        Set<ID> settled = new HashSet<>();
        for (ID startId : nodeMap.keySet()) {
            if (settled.contains(startId)) {
                continue;
            }
            Set<ID> path = new LinkedHashSet<>();
            ID currentId = startId;
            while (currentId != null) {
                TreeNode<ID, V> current = nodeMap.get(currentId);
                if (current == null || settled.contains(currentId)) {
                    break;
                }
                if (!path.add(currentId)) {
                    cycleBreaks.add(currentId);
                    break;
                }
                currentId = current.getParentId();
            }
            settled.addAll(path);
        }
        return cycleBreaks;
    }
}
