package top.egon.cola.component.common.result;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resultDtoJsonKeepsAllFieldsWhenDataIsNull() throws Exception {
        ResultDto<Void> result = ResultDtos.success();

        Map<String, Object> json = toMap(result);

        assertEquals(List.of("success", "code", "status", "message", "data", "traceId", "timestamp"), List.copyOf(json.keySet()));
        assertTrue((Boolean) json.get("success"));
        assertEquals(0, json.get("code"));
        assertTrue(json.containsKey("data"));
        assertTrue(json.containsKey("traceId"));
        assertNotNull(json.get("timestamp"));
        assertFalse(json.containsKey("serialVersionUID"));
    }

    @Test
    void failureJsonDoesNotExposeUnknownExceptionMessage() throws Exception {
        ResultDto<Void> result = ResultDtos.failure(new IllegalStateException("sensitive sql"));

        Map<String, Object> json = toMap(result);

        assertEquals(false, json.get("success"));
        assertEquals(CommonStatus.SYSTEM_ERROR.getCode(), json.get("code"));
        assertEquals(CommonStatus.SYSTEM_ERROR.getStatus(), json.get("status"));
        assertEquals(CommonStatus.SYSTEM_ERROR.getMessage(), json.get("message"));
    }

    @Test
    void pageResultDtoJsonUsesStablePageFieldsAndEmptyRecords() throws Exception {
        PageResultDto<String> result = ResultDtos.page(null, -1, 0, 0);

        Map<String, Object> json = toMap(result);

        assertEquals(List.of(
                "success", "code", "status", "message",
                "records", "total", "pageNo", "pageSize", "pages", "hasNext", "hasPrevious",
                "traceId", "timestamp"
        ), List.copyOf(json.keySet()));
        assertEquals(List.of(), json.get("records"));
        assertEquals(0, json.get("total"));
        assertEquals(1, json.get("pageNo"));
        assertEquals(10, json.get("pageSize"));
        assertEquals(0, json.get("pages"));
    }

    private Map<String, Object> toMap(Object value) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(value), new TypeReference<>() {
        });
    }
}
