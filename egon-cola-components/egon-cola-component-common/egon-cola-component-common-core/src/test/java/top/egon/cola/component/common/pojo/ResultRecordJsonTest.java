package top.egon.cola.component.common.pojo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.enums.ResultCode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultRecordJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resultRecordJsonKeepsAllFieldsWhenDataIsNull() throws Exception {
        ResultRecord<Void> result = ResultRecord.success(null);

        Map<String, Object> json = toMap(result);

        assertEquals(List.of("success", "code", "status", "message", "data", "traceId", "timestamp"), List.copyOf(json.keySet()));
        assertEquals(ResultCode.SUCCESS.getCode(), json.get("code"));
        assertEquals(ResultCode.SUCCESS.getStatus(), json.get("status"));
        assertEquals(ResultCode.SUCCESS.getMessage(), json.get("message"));
        assertTrue((Boolean) json.get("success"));
        assertTrue(json.containsKey("data"));
        assertTrue(json.containsKey("traceId"));
        assertNotNull(json.get("timestamp"));
        assertFalse(json.containsKey("serialVersionUID"));
    }

    @Test
    void failureJsonDoesNotExposeUnknownExceptionMessage() throws Exception {
        ResultRecord<Void> result = ResultRecord.failure(new IllegalStateException("sensitive sql"));

        Map<String, Object> json = toMap(result);

        assertFalse((Boolean) json.get("success"));
        assertEquals(ResultCode.SYSTEM_ERROR.getCode(), json.get("code"));
        assertEquals(ResultCode.SYSTEM_ERROR.getStatus(), json.get("status"));
        assertEquals(ResultCode.SYSTEM_ERROR.getMessage(), json.get("message"));
    }

    @Test
    void pageResultRecordJsonUsesComposedPageMeta() throws Exception {
        PageResultRecord<String> result = PageResultRecord.success(null, -1, 0, 0);

        Map<String, Object> json = toMap(result);
        Map<String, Object> page = page(json);

        assertEquals(List.of("success", "code", "status", "message", "records", "page", "traceId", "timestamp"), List.copyOf(json.keySet()));
        assertEquals(List.of(), json.get("records"));
        assertEquals(ResultCode.SUCCESS.getStatus(), json.get("status"));
        assertEquals(0, page.get("total"));
        assertEquals(1, page.get("pageNo"));
        assertEquals(10, page.get("pageSize"));
        assertEquals(0, page.get("pages"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> page(Map<String, Object> json) {
        return (Map<String, Object>) json.get("page");
    }

    private Map<String, Object> toMap(Object value) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(value), new TypeReference<>() {
        });
    }
}
