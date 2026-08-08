package top.egon.cola.component.ddc.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 将 DDC 文本值转换为支持的标量、枚举、字符串列表或 JSON 对象。 Converts DDC text values into supported scalars, enums, string lists, or JSON objects.
 */
public class DdcValueConverter {

    /**
     * 用于集合和对象 JSON 转换的共享映射器。 Shared mapper used for collection and object JSON conversion.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将配置文本转换为目标 Java 类型。 Converts configuration text to the target Java type.
     *
     * @param value      配置文本。 configuration text
     * @param targetType 目标 Java 类型。 target Java type
     * @param <T>        目标值类型。 target value type
     * @return 已转换的值。 converted value
     * @throws DdcException 数字、枚举或 JSON 转换失败时抛出。 thrown when numeric, enum, or JSON conversion fails
     */
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
