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

        assertTrue(result.isSuccess());
        assertEquals(CommonStatus.SUCCESS.getCode(), result.getCode());
        assertEquals(CommonStatus.SUCCESS.getStatus(), result.getStatus());
        assertEquals("ok", result.getData());
        assertEquals("trace-1", result.getTraceId());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void failureDtoMapsEgonException() {
        ResultDto<Void> result = ResultDtos.failure(new EgonBusinessException(CommonStatus.BAD_REQUEST));

        assertFalse(result.isSuccess());
        assertEquals(CommonStatus.BAD_REQUEST.getCode(), result.getCode());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), result.getStatus());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), result.getMessage());
    }

    @Test
    void pageDtoCalculatesPageMetadata() {
        PageResultDto<String> result = ResultDtos.page(List.of("a"), 11, 2, 10);

        assertTrue(result.isSuccess());
        assertEquals(List.of("a"), result.getRecords());
        assertEquals(11, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(2, result.getPages());
        assertTrue(result.isHasPrevious());
        assertFalse(result.isHasNext());
    }
}
