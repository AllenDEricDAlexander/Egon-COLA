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

        assertTrue(result.success());
        assertEquals(CommonStatus.SUCCESS.getCode(), result.code());
        assertEquals(CommonStatus.SUCCESS.getStatus(), result.status());
        assertEquals("ok", result.data());
    }

    @Test
    void failureModelUsesStatus() {
        ResultModel<Void> result = ResultModels.failure(CommonStatus.BUSINESS_ERROR);

        assertFalse(result.success());
        assertEquals(CommonStatus.BUSINESS_ERROR.getCode(), result.code());
        assertEquals(CommonStatus.BUSINESS_ERROR.getStatus(), result.status());
        assertEquals(CommonStatus.BUSINESS_ERROR.getMessage(), result.message());
    }

    @Test
    void pageModelCalculatesMetadata() {
        PageResultModel<String> result = ResultModels.page(List.of("a"), 1, 1, 10);

        assertTrue(result.success());
        assertEquals(List.of("a"), result.records());
        assertEquals(1, result.total());
        assertEquals(1, result.pageNo());
        assertEquals(10, result.pageSize());
        assertEquals(1, result.pages());
    }
}
