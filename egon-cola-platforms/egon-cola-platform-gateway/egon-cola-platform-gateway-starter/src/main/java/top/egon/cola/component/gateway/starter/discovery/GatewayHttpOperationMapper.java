package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GatewayHttpOperationMapper {

    private final GatewayReportingProperties properties;

    private final ObjectMapper objectMapper;

    GatewayHttpOperationMapper(
            GatewayReportingProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    GatewayDefinitionContributor.DiscoveredInterfaceGroup group(
            Class<?> beanType,
            List<Mapping> mappings) {
        GatewayInterfaceGroup annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        beanType,
                        GatewayInterfaceGroup.class
                );
        if (annotation == null) {
            return null;
        }
        Map<String, GatewayInterfaceDefinitionReport.Operation> operations =
                new LinkedHashMap<>();
        mappings.forEach(mapping -> {
            for (String method : mapping.methods()) {
                for (String path : mapping.paths()) {
                    GatewayInterfaceDefinitionReport.Operation operation =
                            operation(mapping, method, path);
                    GatewayInterfaceDefinitionReport.Operation previous =
                            operations.putIfAbsent(
                                    operation.operationKey(),
                                    operation
                            );
                    if (previous != null
                            && !previous.methodIdentity().equals(
                            operation.methodIdentity())) {
                        throw new IllegalArgumentException(
                                "duplicate HTTP operation key "
                                        + operation.operationKey()
                        );
                    }
                }
            }
        });
        return new GatewayDefinitionContributor.DiscoveredInterfaceGroup(
                annotation.businessDomainCode(),
                annotation.businessDomainName(),
                null,
                annotation.entityDomainCode(),
                annotation.entityDomainName(),
                null,
                new GatewayInterfaceDefinitionReport.InterfaceGroup(
                        annotation.code(),
                        annotation.name(),
                        annotation.description(),
                        "STARTER",
                        beanType.getName(),
                        "HTTP",
                        Map.of(
                                "declaredHosts",
                                properties.getDeclaredHosts()
                        ),
                        new ArrayList<>(operations.values())
                )
        );
    }

    private GatewayInterfaceDefinitionReport.Operation operation(
            Mapping mapping,
            String httpMethod,
            String path) {
        HandlerMethod handler = mapping.handler();
        GatewayOperation annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        handler.getMethod(),
                        GatewayOperation.class
                );
        boolean streaming = mapping.produces().stream()
                .anyMatch(value -> value.contains("text/event-stream"))
                || raw(handler.getMethod().getGenericReturnType())
                .getName()
                .equals("reactor.core.publisher.Flux");
        String operationKey = GatewayOperationKey.http(
                properties.getApplicationCode(),
                httpMethod,
                path
        ).value();
        List<GatewayInterfaceDefinitionReport.Parameter> parameters =
                parameters(handler);
        GatewaySchemaField[] requestDocumentation = annotation == null
                ? new GatewaySchemaField[0]
                : annotation.requestSchemaFields();
        GatewaySchemaField[] responseDocumentation = annotation == null
                ? new GatewaySchemaField[0]
                : annotation.responseSchemaFields();
        Map<String, Object> requestSchema = GatewaySchemaDescriptions.apply(
                bodySchema(parameters),
                requestDocumentation,
                handler.getMethod().getName() + " request"
        );
        Map<String, Object> responseSchema = GatewaySchemaDescriptions.apply(
                schema(responseBodyType(
                        handler.getMethod().getGenericReturnType()
                ), 0),
                responseDocumentation,
                handler.getMethod().getName() + " response"
        );
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("httpMethod", httpMethod);
        attributes.put("path", path);
        attributes.put("consumes", mapping.consumes());
        attributes.put("produces", mapping.produces());
        attributes.put("responseMode", "TRANSPARENT");
        attributes.put("streaming", streaming);
        attributes.put(
                "idempotent",
                GatewayOperationSemantics.idempotent(annotation)
        );
        return new GatewayInterfaceDefinitionReport.Operation(
                operationKey,
                "HTTP",
                httpMethod + " " + path,
                annotation == null || annotation.name().isBlank()
                        ? handler.getMethod().getName()
                        : annotation.name(),
                annotation == null ? "" : annotation.summary(),
                annotation == null ? "" : annotation.description(),
                annotation == null ? "" : annotation.owner(),
                GatewayOperationSemantics.tags(annotation),
                annotation != null && annotation.externalAccessible(),
                streaming ? "UNSUPPORTED" : "SUPPORTED",
                new GatewayInterfaceDefinitionReport.ProviderService(
                        properties.getBizCode(),
                        properties.getApplicationCode(),
                        properties.getEnv(),
                        properties.getNamespace(),
                        "HTTP",
                        properties.getApplicationCode(),
                        "default",
                        properties.getArtifactVersion(),
                        "HTTP"
                ),
                parameters,
                requestSchema,
                responseSchema,
                List.of(),
                null,
                attributes,
                handler.getMethod().isAnnotationPresent(Deprecated.class)
        );
    }

    private List<GatewayInterfaceDefinitionReport.Parameter> parameters(
            HandlerMethod handler) {
        List<GatewayInterfaceDefinitionReport.Parameter> result =
                new ArrayList<>();
        for (MethodParameter parameter : handler.getMethodParameters()) {
            String location = location(parameter);
            if (location == null || frameworkType(
                    parameter.getParameterType()
            )) {
                continue;
            }
            String name = name(parameter);
            boolean required = "PATH".equals(location)
                    || required(parameter);
            result.add(new GatewayInterfaceDefinitionReport.Parameter(
                    name,
                    location,
                    required,
                    parameter.getGenericParameterType().getTypeName(),
                    schema(parameter.getGenericParameterType(), 0),
                    defaultValue(parameter),
                    constraints(parameter),
                    null
            ));
        }
        return result;
    }

    private String location(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(PathVariable.class)) {
            return "PATH";
        }
        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            return "QUERY";
        }
        if (parameter.hasParameterAnnotation(RequestHeader.class)) {
            return "HEADER";
        }
        if (parameter.hasParameterAnnotation(CookieValue.class)) {
            return "COOKIE";
        }
        if (parameter.hasParameterAnnotation(RequestPart.class)) {
            return "PART";
        }
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return "BODY";
        }
        return simple(parameter.getParameterType()) ? "QUERY" : "BODY";
    }

    private String name(MethodParameter parameter) {
        PathVariable path = parameter.getParameterAnnotation(
                PathVariable.class
        );
        if (path != null && !path.name().isBlank()) {
            return path.name();
        }
        RequestParam query = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (query != null && !query.name().isBlank()) {
            return query.name();
        }
        RequestHeader header = parameter.getParameterAnnotation(
                RequestHeader.class
        );
        if (header != null && !header.name().isBlank()) {
            return header.name();
        }
        CookieValue cookie = parameter.getParameterAnnotation(
                CookieValue.class
        );
        if (cookie != null && !cookie.name().isBlank()) {
            return cookie.name();
        }
        RequestPart part = parameter.getParameterAnnotation(
                RequestPart.class
        );
        if (part != null && !part.name().isBlank()) {
            return part.name();
        }
        return parameter.getParameterName() == null
                ? "arg" + parameter.getParameterIndex()
                : parameter.getParameterName();
    }

    private boolean required(MethodParameter parameter) {
        RequestParam query = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (query != null) {
            return query.required();
        }
        RequestHeader header = parameter.getParameterAnnotation(
                RequestHeader.class
        );
        if (header != null) {
            return header.required();
        }
        CookieValue cookie = parameter.getParameterAnnotation(
                CookieValue.class
        );
        if (cookie != null) {
            return cookie.required();
        }
        RequestPart part = parameter.getParameterAnnotation(
                RequestPart.class
        );
        if (part != null) {
            return part.required();
        }
        RequestBody body = parameter.getParameterAnnotation(
                RequestBody.class
        );
        return body != null && body.required();
    }

    private String defaultValue(MethodParameter parameter) {
        RequestParam query = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (query != null
                && !org.springframework.web.bind.annotation.ValueConstants
                .DEFAULT_NONE.equals(query.defaultValue())) {
            return query.defaultValue();
        }
        return null;
    }

    private Map<String, Object> constraints(MethodParameter parameter) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (java.lang.annotation.Annotation annotation
                : parameter.getParameterAnnotations()) {
            String name = annotation.annotationType().getSimpleName();
            if (Set.of(
                    "NotNull",
                    "NotBlank",
                    "NotEmpty",
                    "Size",
                    "Min",
                    "Max",
                    "Pattern",
                    "Positive",
                    "PositiveOrZero"
            ).contains(name)) {
                result.put(name, annotation.toString());
            }
        }
        return result;
    }

    private Map<String, Object> bodySchema(
            List<GatewayInterfaceDefinitionReport.Parameter> parameters) {
        Map<String, Object> body = parameters.stream()
                .filter(parameter -> Set.of("BODY", "PART")
                        .contains(parameter.location()))
                .findFirst()
                .map(GatewayInterfaceDefinitionReport.Parameter::schema)
                .orElse(null);
        if (body != null) {
            return body;
        }
        if (parameters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        parameters.forEach(parameter -> {
            Map<String, Object> field = new LinkedHashMap<>(
                    parameter.schema()
            );
            field.put("location", parameter.location());
            if (parameter.defaultValue() != null) {
                field.put("default", parameter.defaultValue());
            }
            properties.put(parameter.name(), field);
            if (parameter.required()) {
                required.add(parameter.name());
            }
        });
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> schema(Type type, int depth) {
        if (depth > 4) {
            return Map.of("type", "object", "truncated", true);
        }
        Class<?> raw = raw(type);
        if (raw.isEnum()) {
            return Map.of(
                    "type", "string",
                    "enum", java.util.Arrays.stream(raw.getEnumConstants())
                            .map(Object::toString)
                            .toList()
            );
        }
        if (raw == String.class || raw == Character.class
                || raw == char.class) {
            return Map.of("type", "string");
        }
        if (raw == boolean.class || raw == Boolean.class) {
            return Map.of("type", "boolean");
        }
        if (Number.class.isAssignableFrom(raw)
                || raw.isPrimitive() && raw != void.class) {
            return Map.of("type", "number");
        }
        if (raw.isArray() || Collection.class.isAssignableFrom(raw)) {
            Type element = raw.isArray()
                    ? raw.getComponentType()
                    : type instanceof ParameterizedType parameterized
                    ? parameterized.getActualTypeArguments()[0]
                    : Object.class;
            return Map.of(
                    "type", "array",
                    "items", schema(element, depth + 1)
            );
        }
        if (raw == void.class || raw == Void.class) {
            return Map.of("type", "null");
        }
        JavaType javaType = objectMapper.getTypeFactory()
                .constructType(type);
        BeanDescription description = objectMapper.getSerializationConfig()
                .introspect(javaType);
        Map<String, Object> properties = new LinkedHashMap<>();
        description.findProperties().stream()
                .limit(200)
                .forEach(property -> {
                    JavaType propertyType = property.getPrimaryType();
                    properties.put(
                            property.getName(),
                            schema(propertyType.getRawClass(), depth + 1)
                    );
                });
        return Map.of(
                "type", "object",
                "javaType", raw.getName(),
                "properties", properties
        );
    }

    private Type responseBodyType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = raw(type);
            if (raw.getName().equals("reactor.core.publisher.Mono")
                    || HttpEntity.class.isAssignableFrom(raw)) {
                return parameterized.getActualTypeArguments()[0];
            }
        }
        return type;
    }

    private boolean simple(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class;
    }

    private boolean frameworkType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("jakarta.servlet.")
                || name.startsWith("javax.servlet.")
                || name.startsWith("org.springframework.http.server.")
                || name.startsWith("org.springframework.web.server.")
                || java.security.Principal.class.isAssignableFrom(type);
    }

    private Class<?> raw(Type type) {
        if (type instanceof Class<?> value) {
            return value;
        }
        if (type instanceof ParameterizedType parameterized
                && parameterized.getRawType() instanceof Class<?> value) {
            return value;
        }
        return Object.class;
    }

    record Mapping(
            HandlerMethod handler,
            Set<String> paths,
            Set<String> methods,
            Set<String> consumes,
            Set<String> produces
    ) {
    }
}
