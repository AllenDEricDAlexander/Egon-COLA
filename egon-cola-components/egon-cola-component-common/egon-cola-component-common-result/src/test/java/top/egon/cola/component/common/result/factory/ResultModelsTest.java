package top.egon.cola.component.common.result.factory;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.result.model.PageResultModel;
import top.egon.cola.component.common.result.model.ResultModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultModelsTest {

    @Test
    void successModelDoesNotCarryTraceFields() {
        ResultModel<String> result = ResultModels.success("ok");

        assertTrue(result.isSuccess());
        assertEquals(CommonStatus.SUCCESS.getCode(), result.getCode());
        assertEquals(CommonStatus.SUCCESS.getStatus(), result.getStatus());
        assertEquals("ok", result.getData());
    }

    @Test
    void failureModelUsesStatus() {
        ResultModel<Void> result = ResultModels.failure(CommonStatus.BUSINESS_ERROR);

        assertFalse(result.isSuccess());
        assertEquals(CommonStatus.BUSINESS_ERROR.getCode(), result.getCode());
        assertEquals(CommonStatus.BUSINESS_ERROR.getStatus(), result.getStatus());
        assertEquals(CommonStatus.BUSINESS_ERROR.getMessage(), result.getMessage());
    }

    @Test
    void pageModelCalculatesMetadata() {
        PageResultModel<String> result = ResultModels.page(List.of("a"), 1, 1, 10);

        assertTrue(result.isSuccess());
        assertEquals(List.of("a"), result.getRecords());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getPages());
    }
}
