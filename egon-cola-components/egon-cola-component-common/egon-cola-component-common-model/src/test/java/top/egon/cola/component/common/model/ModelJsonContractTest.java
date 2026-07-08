package top.egon.cola.component.common.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.model.page.PageModel;
import top.egon.cola.component.common.model.query.PageQuery;
import top.egon.cola.component.common.model.query.SortQuery;
import top.egon.cola.component.common.model.query.TimeRangeQuery;
import top.egon.cola.component.common.model.request.BaseRequest;
import top.egon.cola.component.common.model.request.OperatorContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pageQueryJsonUsesStableFieldsAndHidesOffset() throws Exception {
        Map<String, Object> json = toMap(new PageQuery(0, 999));

        assertEquals(List.of("pageNo", "pageSize"), List.copyOf(json.keySet()));
        assertEquals(1, json.get("pageNo"));
        assertEquals(PageQuery.MAX_PAGE_SIZE, json.get("pageSize"));
        assertFalse(json.containsKey("offset"));
        assertFalse(json.containsKey("serialVersionUID"));
    }

    @Test
    void pageModelJsonKeepsRecordsAndMeta() throws Exception {
        Map<String, Object> json = toMap(PageModel.of(null, 0, 0, 0));

        assertEquals(List.of("records", "meta"), List.copyOf(json.keySet()));
        assertEquals(List.of(), json.get("records"));
        assertTrue(json.containsKey("meta"));
    }

    @Test
    void requestJsonKeepsNullOperatorAndOperatorFields() throws Exception {
        Map<String, Object> requestJson = toMap(new BaseRequest(null));
        Map<String, Object> operatorJson = toMap(new OperatorContext("u1", null, "t1"));

        assertEquals(List.of("operator"), List.copyOf(requestJson.keySet()));
        assertTrue(requestJson.containsKey("operator"));
        assertEquals(List.of("operatorId", "operatorName", "tenantId"), List.copyOf(operatorJson.keySet()));
        assertTrue(operatorJson.containsKey("operatorName"));
    }

    @Test
    void sortAndTimeRangeJsonUseStableFields() throws Exception {
        Map<String, Object> sortJson = toMap(new SortQuery(" name ", "desc"));
        Map<String, Object> timeJson = toMap(new TimeRangeQuery(null, null));

        assertEquals("name", sortJson.get("sortBy"));
        assertEquals("DESC", sortJson.get("sortDirection"));
        assertEquals(List.of("startTime", "endTime"), List.copyOf(timeJson.keySet()));
    }

    private Map<String, Object> toMap(Object value) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(value), new TypeReference<>() {
        });
    }
}
