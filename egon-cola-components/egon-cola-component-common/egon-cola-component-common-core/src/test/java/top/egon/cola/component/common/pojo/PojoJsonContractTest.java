package top.egon.cola.component.common.pojo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.BaseRequest;
import top.egon.cola.component.common.core.pojo.OperatorContext;
import top.egon.cola.component.common.core.pojo.PageMetaRecord;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageSlice;
import top.egon.cola.component.common.core.pojo.SortQuery;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PojoJsonContractTest {

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
    void pageMetaJsonUsesStableFields() throws Exception {
        Map<String, Object> json = toMap(PageMetaRecord.of(21, 2, 10));

        assertEquals(List.of("total", "pageNo", "pageSize", "pages", "hasNext", "hasPrevious"), List.copyOf(json.keySet()));
        assertEquals(21, json.get("total"));
        assertEquals(2, json.get("pageNo"));
        assertEquals(10, json.get("pageSize"));
        assertEquals(3, json.get("pages"));
        assertTrue((Boolean) json.get("hasNext"));
        assertTrue((Boolean) json.get("hasPrevious"));
    }

    @Test
    void pageSliceJsonKeepsRecordsAndHasNext() throws Exception {
        Map<String, Object> json = toMap(PageSlice.of(null, true));

        assertEquals(List.of("records", "hasNext"), List.copyOf(json.keySet()));
        assertEquals(List.of(), json.get("records"));
        assertTrue((Boolean) json.get("hasNext"));
    }

    @Test
    void requestJsonKeepsNullOperatorAndUserFields() throws Exception {
        Map<String, Object> requestJson = toMap(new BaseRequest(null));
        Map<String, Object> operatorJson = toMap(new OperatorContext("u1", null, "t1"));

        assertEquals(List.of("operator"), List.copyOf(requestJson.keySet()));
        assertTrue(requestJson.containsKey("operator"));
        assertEquals(List.of("userId", "userName", "tenantId"), List.copyOf(operatorJson.keySet()));
        assertTrue(operatorJson.containsKey("userName"));
    }

    @Test
    void sortJsonUsesStableFields() throws Exception {
        Map<String, Object> sortJson = toMap(new SortQuery(" name ", "desc"));

        assertEquals(List.of("sortBy", "sortDirection"), List.copyOf(sortJson.keySet()));
        assertEquals("name", sortJson.get("sortBy"));
        assertEquals("DESC", sortJson.get("sortDirection"));
        assertFalse(sortJson.containsKey("hasSort"));
    }

    private Map<String, Object> toMap(Object value) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(value), new TypeReference<>() {
        });
    }
}
