package top.egon.cola.component.common.structure.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic mutable tree node used by TreeBuilder.
 */
public class TreeNode<ID, V> {

    private final ID id;

    private final ID parentId;

    private final V value;

    private final List<TreeNode<ID, V>> children = new ArrayList<>();

    public TreeNode(ID id, ID parentId, V value) {
        this.id = id;
        this.parentId = parentId;
        this.value = value;
    }

    public ID getId() {
        return id;
    }

    public ID getParentId() {
        return parentId;
    }

    public V getValue() {
        return value;
    }

    public List<TreeNode<ID, V>> getChildren() {
        return children;
    }

    public void addChild(TreeNode<ID, V> child) {
        children.add(child);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TreeNode<?, ?> treeNode)) {
            return false;
        }
        return Objects.equals(id, treeNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
