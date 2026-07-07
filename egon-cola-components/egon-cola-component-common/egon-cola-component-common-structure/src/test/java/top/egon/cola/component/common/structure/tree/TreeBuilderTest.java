package top.egon.cola.component.common.structure.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeBuilderTest {

    @Test
    void buildsTreeFromFlatNodes() {
        TreeNode<Long, String> root = new TreeNode<>(1L, null, "root");
        TreeNode<Long, String> child = new TreeNode<>(2L, 1L, "child");
        TreeNode<Long, String> another = new TreeNode<>(3L, null, "another");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(child, root, another));

        assertEquals(List.of(root, another), roots);
        assertEquals(List.of(child), root.getChildren());
    }

    @Test
    void keepsOrphansAsRootsByDefault() {
        TreeNode<Long, String> orphan = new TreeNode<>(2L, 99L, "orphan");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(orphan));

        assertEquals(List.of(orphan), roots);
    }
}
