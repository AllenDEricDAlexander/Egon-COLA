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

final class GatewayResponseSchemaMapper {

    private static final String RESULT_RECORD =
            "top.egon.cola.component.common.core.pojo.ResultRecord";

    private static final String PAGE_RESULT_RECORD =
            "top.egon.cola.component.common.core.pojo.PageResultRecord";

    private final ObjectMapper objectMapper;

    private final GatewayJavaSchemaMapper schemaMapper;

    GatewayResponseSchemaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaMapper = new GatewayJavaSchemaMapper(objectMapper);
    }

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

    private boolean defaultDeclaration(GatewayResponseSchema declaration) {
        return declaration.wrapper() == Void.class
                && declaration.payloadField().isBlank()
                && declaration.schema() == Void.class
                && declaration.shape() == GatewaySchemaShape.AUTO;
    }

    private boolean sameType(Class<?> left, Class<?> right) {
        return box(left).equals(box(right));
    }

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            return new LinkedHashMap<>();
        }
        return (Map<String, Object>) value;
    }

    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                value
        ));
        return result;
    }
}
