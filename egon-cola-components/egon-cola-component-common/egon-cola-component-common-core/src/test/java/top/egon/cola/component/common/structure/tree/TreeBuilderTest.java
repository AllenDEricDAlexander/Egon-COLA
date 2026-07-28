package top.egon.cola.component.common.structure.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void selfParentBecomesRootInsteadOfBeingDropped() {
        TreeNode<Long, String> self = new TreeNode<>(1L, 1L, "self");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(self));

        assertEquals(List.of(self), roots);
        assertTrue(self.getChildren().isEmpty());
    }

    @Test
    void cycleKeepsEveryNodeReachable() {
        TreeNode<Long, String> a = new TreeNode<>(1L, 2L, "a");
        TreeNode<Long, String> b = new TreeNode<>(2L, 1L, "b");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(a, b));

        assertEquals(List.of(a), roots);
        assertEquals(List.of(b), a.getChildren());
    }

    @Test
    void nodeHangingOffACycleStaysAttached() {
        TreeNode<Long, String> a = new TreeNode<>(1L, 2L, "a");
        TreeNode<Long, String> b = new TreeNode<>(2L, 1L, "b");
        TreeNode<Long, String> c = new TreeNode<>(3L, 2L, "c");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(a, b, c));

        assertEquals(List.of(a), roots);
        assertEquals(List.of(b), a.getChildren());
        assertEquals(List.of(c), b.getChildren());
    }

    @Test
    void failOnCycleRejectsCyclicInput() {
        TreeNode<Long, String> a = new TreeNode<>(1L, 2L, "a");
        TreeNode<Long, String> b = new TreeNode<>(2L, 1L, "b");
        TreeOptions options = new TreeOptions();
        options.setFailOnCycle(true);

        assertThrows(IllegalArgumentException.class, () -> TreeBuilder.build(List.of(a, b), options));
    }
}
