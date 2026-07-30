package top.egon.cola.component.ddc.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.math.BigDecimal;
import java.util.List;

public class DdcValueConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T convert(String value, Class<T> targetType) {
        try {
            if (targetType == String.class) {
                return (T) value;
            }
            if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(value);
            }
            if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(value);
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return (T) Boolean.valueOf(value);
            }
            if (targetType == Double.class || targetType == double.class) {
                return (T) Double.valueOf(value);
            }
            if (targetType == BigDecimal.class) {
                return (T) new BigDecimal(value);
            }
            if (targetType.isEnum()) {
                return (T) Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), value);
            }
            if (targetType == List.class) {
                CollectionType type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class);
                return OBJECT_MAPPER.readValue(value, type);
            }
            return OBJECT_MAPPER.readValue(value, targetType);
        } catch (Exception e) {
            throw new DdcException("convert config value failed", e);
        }
    }
}
