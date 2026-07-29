package top.egon.cola.component.common.core;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.common.core.pojo.TreeBuilder;
import top.egon.cola.component.common.core.pojo.TreeNode;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CoreConsolidationTest {

    @Test
    void corePublishesPojoResultTraceAndStructureContracts() {
        TraceContext.setTraceId("trace-core");
        try {
            PageResultRecord<String> pageResult = PageResultRecord.success(List.of("one"), 1, 2, 20);
            ResultRecord<PageResultRecord<String>> result = ResultRecord.success(pageResult);

            TreeNode<Long, String> root = new TreeNode<>(1L, 0L, "root");
            TreeNode<Long, String> child = new TreeNode<>(2L, 1L, "child");
            List<TreeNode<Long, String>> tree = TreeBuilder.build(List.of(root, child));

            assertEquals("trace-core", result.traceId());
            assertEquals(1, result.data().records().size());
            assertEquals(1, result.data().page().total());
            assertEquals(1, tree.size());
            assertEquals(1, tree.get(0).getChildren().size());
            assertNotNull(TraceContext.snapshot());
        } finally {
            TraceContext.clearTraceId();
        }
    }
}
