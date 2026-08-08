package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.springframework.http.HttpEntity;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps HTTP handler return types to Gateway response JSON Schemas and validates
 * explicit response schema declarations.
 * 中文说明：根据处理器的真实返回类型生成响应 Schema，并校验注解中的显式声明。
 */
final class GatewayResponseSchemaMapper {

    /** Fully qualified name of the standard single-result response wrapper. 单结果包装器的全限定类名。 */
    private static final String RESULT_RECORD =
            "top.egon.cola.component.common.core.pojo.ResultRecord";

    /** Fully qualified name of the standard paginated response wrapper. 分页结果包装器的全限定类名。 */
    private static final String PAGE_RESULT_RECORD =
            "top.egon.cola.component.common.core.pojo.PageResultRecord";

    /** Jackson mapper used to resolve generic return and wrapper property types. 用于解析泛型返回值及包装器属性类型。 */
    private final ObjectMapper objectMapper;

    /** Mapper that converts resolved Java types to JSON Schema documents. 将解析后的 Java 类型转换为 JSON Schema 文档。 */
    private final GatewayJavaSchemaMapper schemaMapper;

    /**
     * Creates a response schema mapper.
     * 中文说明：初始化类型解析器，并创建 Java 类型到 Schema 的映射器。
     *
     * @param objectMapper Jackson mapper used for type resolution
     */
    GatewayResponseSchemaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaMapper = new GatewayJavaSchemaMapper(objectMapper);
    }

    /**
     * Generates the response schema for a handler method and validates any
     * explicit declaration supplied by the Gateway operation.
     * 中文说明：先剥离 HTTP/响应式容器，再按操作声明校验并生成响应 Schema。
     *
     * @param method handler method whose return type is mapped
     * @param operation Gateway operation declaration, or {@code null}
     * @return generated response JSON Schema
     * @throws IllegalArgumentException if an MCP schema is missing or a
     *         declaration does not match the handler return type
     */
    Map<String, Object> schema(Method method, GatewayOperation operation) {
        JavaType returnType = responseBodyType(
                objectMapper.constructType(method.getGenericReturnType())
        );
        GatewayResponseSchema declaration = operation == null
                ? null : operation.responseSchema();
        boolean explicit = declaration != null && !defaultDeclaration(
                declaration
        );
        if (operation != null && operation.registerMcp()
                && schemaMapper.shape(returnType) != GatewaySchemaShape.VOID
                && !explicit) {
            throw new IllegalArgumentException(
                    "registerMcp operation requires explicit responseSchema: "
                            + method.toGenericString()
            );
        }
        if (explicit) {
            validate(returnType, declaration, method.toGenericString());
        }
        Map<String, Object> result = new LinkedHashMap<>(
                schemaMapper.schema(returnType)
        );
        result.put(
                "x-egon-schema-model",
                "gateway-operation-response/v2"
        );
        applyWrapperSemantics(result, returnType, declaration);
        return result;
    }

    /**
     * Validates an explicit response declaration against the resolved return
     * type, including direct, wrapped, and void responses.
     * 中文说明：覆盖直接返回、标准包装返回和 void 返回三种声明形态。
     *
     * @param actualType resolved handler response type
     * @param declaration explicit response schema declaration
     * @param identity method identity used in validation errors
     * @throws IllegalArgumentException if the declaration is inconsistent
     */
    private void validate(
            JavaType actualType,
            GatewayResponseSchema declaration,
            String identity) {
        GatewaySchemaShape actualShape = schemaMapper.shape(actualType);
        if (actualShape == GatewaySchemaShape.VOID) {
            if (declaration.wrapper() != Void.class
                    || declaration.schema() != Void.class
                    || declaration.shape() != GatewaySchemaShape.VOID
                    || !declaration.payloadField().isBlank()) {
                throw new IllegalArgumentException(
                        identity + " void responseSchema is invalid"
                );
            }
            return;
        }
        if (declaration.wrapper() == Void.class) {
            if (!declaration.payloadField().isBlank()) {
                throw new IllegalArgumentException(
                        identity + " direct response cannot declare payloadField"
                );
            }
            if (declaration.schema() == Void.class) {
                throw new IllegalArgumentException(
                        identity + " response schema class is required"
                );
            }
            schemaMapper.validateDeclaration(
                    actualType,
                    declaration.schema(),
                    declaration.shape(),
                    identity + " response"
            );
            return;
        }
        if (!sameType(actualType.getRawClass(), declaration.wrapper())) {
            throw new IllegalArgumentException(
                    identity + " response wrapper mismatch: declared="
                            + declaration.wrapper().getName() + ", actual="
                            + actualType.toCanonical()
            );
        }
        if (declaration.payloadField().isBlank()) {
            throw new IllegalArgumentException(
                    identity + " response payloadField is required"
            );
        }
        if (declaration.schema() == Void.class) {
            throw new IllegalArgumentException(
                    identity + " response payload schema is required"
            );
        }
        JavaType payloadType = propertyType(
                actualType,
                declaration.payloadField(),
                identity
        );
        schemaMapper.validateDeclaration(
                payloadType,
                declaration.schema(),
                declaration.shape(),
                identity + " response payload " + declaration.payloadField()
        );
    }

    /**
     * Resolves the serializable type of a declared wrapper payload property.
     * 中文说明：通过 Jackson 序列化属性查找包装器中真正可输出的载荷类型。
     *
     * @param wrapperType resolved wrapper type
     * @param payloadField payload property name
     * @param identity method identity used in validation errors
     * @return resolved payload property type
     * @throws IllegalArgumentException if the property is not serializable or
     *         does not exist
     */
    private JavaType propertyType(
            JavaType wrapperType,
            String payloadField,
            String identity) {
        BeanDescription bean = objectMapper.getSerializationConfig()
                .introspect(wrapperType);
        return bean.findProperties().stream()
                .filter(BeanPropertyDefinition::couldSerialize)
                .filter(property -> property.getName().equals(payloadField))
                .findFirst()
                .map(BeanPropertyDefinition::getPrimaryType)
                .orElseThrow(() -> new IllegalArgumentException(
                        identity + " response payloadField does not exist: "
                                + payloadField
                ));
    }

    /**
     * Applies the required-field and nullable-payload rules of standard COLA
     * result wrappers to a generated schema.
     * 中文说明：标准 ResultRecord 的载荷允许为 null，包装器字段整体按属性名标记为必填。
     *
     * @param schema generated schema to update
     * @param returnType resolved response type
     * @param declaration response declaration, or {@code null}
     */
    private void applyWrapperSemantics(
            Map<String, Object> schema,
            JavaType returnType,
            GatewayResponseSchema declaration) {
        String className = returnType.getRawClass().getName();
        if (!RESULT_RECORD.equals(className)
                && !PAGE_RESULT_RECORD.equals(className)) {
            return;
        }
        Map<String, Object> properties = map(schema.get("properties"));
        schema.put("required", new ArrayList<>(properties.keySet()));
        if (RESULT_RECORD.equals(className)) {
            String payload = declaration == null
                    || declaration.payloadField().isBlank()
                    ? "data" : declaration.payloadField();
            Object payloadSchema = properties.get(payload);
            if (payloadSchema instanceof Map<?, ?> map
                    && !map.containsKey("anyOf")) {
                properties.put(payload, Map.of(
                        "anyOf",
                        List.of(copyMap(map), Map.of("type", "null"))
                ));
            }
        }
    }

    /**
     * Unwraps supported HTTP and reactive response containers to the logical
     * response body type.
     * 中文说明：HttpEntity、Mono 取内容类型，Flux 则转换为列表类型。
     *
     * @param type declared handler return type
     * @return logical body type, with {@code Flux} represented as a list
     */
    private JavaType responseBodyType(JavaType type) {
        Class<?> raw = type.getRawClass();
        if (HttpEntity.class.isAssignableFrom(raw)
                || raw.getName().equals("reactor.core.publisher.Mono")) {
            JavaType content = type.containedType(0);
            return content == null
                    ? objectMapper.constructType(Void.class) : content;
        }
        if (raw.getName().equals("reactor.core.publisher.Flux")) {
            JavaType content = type.containedType(0);
            return objectMapper.getTypeFactory().constructCollectionType(
                    List.class,
                    content == null
                            ? objectMapper.constructType(Object.class) : content
            );
        }
        return type;
    }

    /**
     * Determines whether a response declaration contains only annotation
     * defaults.
     * 中文说明：仅当包装器、载荷字段、Schema 类和形状均为默认值时才视为未显式声明。
     *
     * @param declaration declaration to inspect
     * @return {@code true} when no explicit schema values were supplied
     */
    private boolean defaultDeclaration(GatewayResponseSchema declaration) {
        return declaration.wrapper() == Void.class
                && declaration.payloadField().isBlank()
                && declaration.schema() == Void.class
                && declaration.shape() == GatewaySchemaShape.AUTO;
    }

    /**
     * Compares two classes after normalizing primitive types to wrappers.
     * 中文说明：比较声明类型时先把基本类型统一为对应包装类型。
     *
     * @param left first class
     * @param right second class
     * @return {@code true} when both classes represent the same logical type
     */
    private boolean sameType(Class<?> left, Class<?> right) {
        return box(left).equals(box(right));
    }

    /**
     * Converts a primitive class to its wrapper class.
     * 中文说明：将 boolean、数值和 char 等基本类型映射为包装类，非基本类型保持不变。
     *
     * @param type class to normalize
     * @return wrapper class for a primitive, or the original class otherwise
     */
    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    /**
     * Returns a schema value as a string-keyed map when possible.
     * 中文说明：仅接受 Map 作为可变 Schema 节点，否则返回新的空映射。
     *
     * @param value candidate map value
     * @return cast map, or an empty mutable map when the value is not a map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            return new LinkedHashMap<>();
        }
        return (Map<String, Object>) value;
    }

    /**
     * Creates a mutable shallow copy with stringified keys.
     * 中文说明：复制顶层键值并把键转换为字符串，嵌套值不做深拷贝。
     *
     * @param source map to copy
     * @return mutable string-keyed copy
     */
    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                value
        ));
        return result;
    }
}
