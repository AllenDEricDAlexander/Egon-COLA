package top.egon.cola.component.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.common.exception.SystemException;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具门面，统一收口 Jackson 调用和异常包装。
 */
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new SystemException("JSON_SERIALIZE_ERROR", "JSON 序列化失败", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new SystemException("JSON_DESERIALIZE_ERROR", "JSON 反序列化失败", e);
        }
    }

    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            throw new SystemException("JSON_DESERIALIZE_ERROR", "JSON 反序列化失败", e);
        }
    }

    public static Map<String, Object> toMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new SystemException("JSON_DESERIALIZE_ERROR", "JSON 反序列化失败", e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {
        return OBJECT_MAPPER.convertValue(value, type);
    }
}
