package top.egon.cola.component.common.result;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.exception.BusinessException;
import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.trace.TraceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void successResultCarriesTraceId() {
        TraceContext.setTraceId("trace-001");

        Result<String> result = Result.success("ok");

        assertTrue(result.isSuccess());
        assertEquals(ErrorCodes.SUCCESS.getCode(), result.getCode());
        assertEquals(ErrorCodes.SUCCESS.getMessage(), result.getMessage());
        assertEquals("ok", result.getData());
        assertEquals("trace-001", result.getTraceId());
    }

    @Test
    void failureResultCarriesTraceId() {
        TraceContext.setTraceId("trace-002");

        Result<String> result = Result.failure("ORDER_NOT_FOUND", "订单不存在");

        assertFalse(result.isSuccess());
        assertEquals("ORDER_NOT_FOUND", result.getCode());
        assertEquals("订单不存在", result.getMessage());
        assertNull(result.getData());
        assertEquals("trace-002", result.getTraceId());
    }

    @Test
    void failureFromBusinessExceptionUsesExceptionCode() {
        Result<Void> result = Result.failure(new BusinessException("ORDER_NOT_FOUND", "订单不存在"));

        assertFalse(result.isSuccess());
        assertEquals("ORDER_NOT_FOUND", result.getCode());
        assertEquals("订单不存在", result.getMessage());
    }
}
