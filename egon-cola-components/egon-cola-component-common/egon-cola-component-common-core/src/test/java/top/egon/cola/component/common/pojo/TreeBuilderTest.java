package top.egon.cola.component.common.pojo;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.BasePojo;
import top.egon.cola.component.common.core.pojo.TreeBuilder;
import top.egon.cola.component.common.core.pojo.TreeNode;
import top.egon.cola.component.common.core.pojo.TreeOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    @Test
    void treeCarriersCarryTheCommonCarrierContract() throws Exception {
        TreeNode<Long, String> node = new TreeNode<>(1L, null, "root");
        TreeOptions options = new TreeOptions().setFailOnCycle(true);

        assertInstanceOf(BasePojo.class, node);
        assertInstanceOf(BasePojo.class, options);
        assertEquals(node, TreeBuilderTest.<TreeNode<Long, String>>deserialize(serialize(node)));
        TreeOptions restoredOptions = TreeBuilderTest.<TreeOptions>deserialize(serialize(options));
        assertTrue(restoredOptions.isFailOnCycle());
        assertTrue(restoredOptions.isKeepOrphansAsRoots());
    }

    private static <T extends Serializable> byte[] serialize(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) input.readObject();
        }
    }
}
