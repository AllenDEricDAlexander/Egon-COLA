package top.egon.cola.component.common.result;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void buildsPageMetadataAndTraceId() {
        TraceContext.setTraceId("trace-page");

        PageResult<String> result = PageResult.success(List.of("a", "b"), 5, 2, 2);

        assertTrue(result.isSuccess());
        assertEquals(ErrorCodes.SUCCESS.getCode(), result.getCode());
        assertEquals(ErrorCodes.SUCCESS.getMessage(), result.getMessage());
        assertEquals(List.of("a", "b"), result.getRecords());
        assertEquals(5, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getPages());
        assertTrue(result.isHasPrevious());
        assertTrue(result.isHasNext());
        assertEquals("trace-page", result.getTraceId());
    }

    @Test
    void normalizesEmptyRecordsAndPageValues() {
        PageResult<String> result = PageResult.success(null, 0, 0, 0);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(1, result.getPageNo());
        assertEquals(1, result.getPageSize());
        assertEquals(0, result.getPages());
        assertFalse(result.isHasPrevious());
        assertFalse(result.isHasNext());
    }
}
