package top.egon.cola.component.common.structure.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            parent.addChild(node);
        }
        return roots;
    }
}
