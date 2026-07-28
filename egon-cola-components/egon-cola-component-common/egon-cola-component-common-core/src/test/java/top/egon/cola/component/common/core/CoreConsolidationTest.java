package top.egon.cola.component.common.core;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.model.page.PageModel;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.common.structure.tree.TreeBuilder;
import top.egon.cola.component.common.structure.tree.TreeNode;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CoreConsolidationTest {

    @Test
    void corePublishesModelResultTraceAndStructureContracts() {
        PageModel<String> pageModel = PageModel.of(List.of("one"), 1, 2, 20);

        TraceContext.setTraceId("trace-core");
        ResultDto<PageModel<String>> result = ResultDtos.success(pageModel);

        TreeNode<Long, String> root = new TreeNode<>(1L, 0L, "root");
        TreeNode<Long, String> child = new TreeNode<>(2L, 1L, "child");
        List<TreeNode<Long, String>> tree = TreeBuilder.build(List.of(root, child));

        assertEquals("trace-core", result.traceId());
        assertEquals(1, result.data().records().size());
        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).getChildren().size());
        assertNotNull(TraceContext.snapshot());
        TraceContext.clearTraceId();
    }
}
