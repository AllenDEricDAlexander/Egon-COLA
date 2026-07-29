package top.egon.cola.component.common.pojo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.exception.RemoteCallException;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonPojoContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resultRecordUsesResultCodeAndTraceContext() throws Exception {
        TraceContext.setTraceId("trace-pojo");

        ResultRecord<String> result = ResultRecord.success("ok");

        Map<String, Object> json = toMap(result);
        assertEquals(List.of("success", "code", "status", "message", "data", "traceId", "timestamp"), List.copyOf(json.keySet()));
        assertEquals(ResultCode.SUCCESS.getCode(), result.code());
        assertEquals(ResultCode.SUCCESS.getStatus(), result.status());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.message());
        assertTrue(result.success());
        assertEquals("ok", result.data());
        assertEquals("trace-pojo", result.traceId());
        assertNotNull(result.timestamp());

        TraceContext.clearTraceId();
    }

    @Test
    void failureResultRecordDoesNotExposeUnknownExceptionMessage() {
        ResultRecord<Void> result = ResultRecord.failure(new IllegalStateException("database password leaked"));

        assertFalse(result.success());
        assertEquals(ResultCode.SYSTEM_ERROR.getCode(), result.code());
        assertEquals(ResultCode.SYSTEM_ERROR.getStatus(), result.status());
        assertEquals(ResultCode.SYSTEM_ERROR.getMessage(), result.message());
    }

    @Test
    void pageResultRecordComposesPageMetaRecord() throws Exception {
        PageResultRecord<String> result = PageResultRecord.success(List.of("a"), 11, 2, 10);

        Map<String, Object> json = toMap(result);
        assertEquals(List.of("success", "code", "status", "message", "records", "page", "traceId", "timestamp"), List.copyOf(json.keySet()));
        assertEquals(List.of("a"), result.records());
        assertEquals(ResultCode.SUCCESS.getStatus(), result.status());
        assertEquals(11, result.page().total());
        assertEquals(2, result.page().pageNo());
        assertEquals(10, result.page().pageSize());
        assertEquals(2, result.page().pages());
        assertFalse(result.page().hasNext());
        assertTrue(result.page().hasPrevious());

        assertInstanceOf(Map.class, json.get("page"));
    }

    @Test
    void commonExceptionsDoNotUseEgonClassPrefix() {
        BusinessException business = new BusinessException(ResultCode.INVALID_PARAMS);
        RuntimeException cause = new RuntimeException("timeout");
        RemoteCallException remote = new RemoteCallException(ResultCode.REMOTE_CALL_ERROR, true, cause);

        assertEquals("BusinessException", business.getClass().getSimpleName());
        assertEquals(ResultCode.INVALID_PARAMS.getCode(), business.getCode());
        assertEquals(ResultCode.INVALID_PARAMS.getStatus(), business.getStatus());
        assertEquals(ResultCode.INVALID_PARAMS.getMessage(), business.getMessage());
        assertFalse(business.isRetryable());
        assertEquals("RemoteCallException", remote.getClass().getSimpleName());
        assertTrue(remote.isRetryable());
        assertSame(cause, remote.getCause());
    }

    private Map<String, Object> toMap(Object value) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(value), new TypeReference<>() {
        });
    }
}
