package top.egon.cola.component.common.result.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.core.exception.EgonBusinessException;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultDtosTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void successDtoCarriesTraceIdAndTimestamp() {
        TraceContext.setTraceId("trace-1");

        ResultDto<String> result = ResultDtos.success("ok");

        assertTrue(result.success());
        assertEquals(CommonStatus.SUCCESS.getCode(), result.code());
        assertEquals(CommonStatus.SUCCESS.getStatus(), result.status());
        assertEquals("ok", result.data());
        assertEquals("trace-1", result.traceId());
        assertNotNull(result.timestamp());
    }

    @Test
    void failureDtoMapsEgonException() {
        ResultDto<Void> result = ResultDtos.failure(new EgonBusinessException(CommonStatus.BAD_REQUEST));

        assertFalse(result.success());
        assertEquals(CommonStatus.BAD_REQUEST.getCode(), result.code());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), result.status());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), result.message());
    }

    @Test
    void failureDtoHidesUnknownExceptionMessage() {
        ResultDto<Void> result = ResultDtos.failure(new NullPointerException("database password leaked"));

        assertFalse(result.success());
        assertEquals(CommonStatus.SYSTEM_ERROR.getCode(), result.code());
        assertEquals(CommonStatus.SYSTEM_ERROR.getStatus(), result.status());
        assertEquals(CommonStatus.SYSTEM_ERROR.getMessage(), result.message());
    }

    @Test
    void pageDtoCalculatesPageMetadata() {
        PageResultDto<String> result = ResultDtos.page(List.of("a"), 11, 2, 10);

        assertTrue(result.success());
        assertEquals(List.of("a"), result.records());
        assertEquals(11, result.total());
        assertEquals(2, result.pageNo());
        assertEquals(10, result.pageSize());
        assertEquals(2, result.pages());
        assertTrue(result.hasPrevious());
        assertFalse(result.hasNext());
    }
}
