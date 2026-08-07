package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestSchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaRequired;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GatewayRequestSchemaValidator {

    private static final List<GatewayRequestLocation> HTTP_LOCATIONS = List.of(
            GatewayRequestLocation.PATH,
            GatewayRequestLocation.QUERY,
            GatewayRequestLocation.HEADER,
            GatewayRequestLocation.COOKIE,
            GatewayRequestLocation.BODY,
            GatewayRequestLocation.PART
    );

    private final ObjectMapper objectMapper;

    private final GatewayJavaSchemaMapper schemaMapper;

    GatewayRequestSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaMapper = new GatewayJavaSchemaMapper(objectMapper);
    }

    Result validate(
            HandlerMethod handler,
            GatewayOperation operation,
            String routePath) {
        List<RequestParameter> actual = parameters(handler, routePath);
        long bodyCount = actual.stream()
                .filter(parameter -> parameter.location()
                        == GatewayRequestLocation.BODY)
                .count();
        if (bodyCount > 1) {
            throw invalid(handler, "multiple request bodies are unsupported");
        }
        GatewayRequestSchemaField[] declarations = operation == null
                ? new GatewayRequestSchemaField[0]
                : operation.requestSchemaFields();
        List<Binding> bindings;
        if (declarations.length == 0) {
            if (operation != null && operation.registerMcp()
                    && !actual.isEmpty()) {
                throw invalid(
                        handler,
                        "registerMcp requires complete requestSchemaFields"
                );
            }
            bindings = actual.stream()
                    .map(parameter -> new Binding(parameter, null))
                    .toList();
        } else {
            bindings = bind(handler, actual, declarations);
        }
        return new Result(schema(bindings), List.copyOf(actual));
    }

    private List<Binding> bind(
            HandlerMethod handler,
            List<RequestParameter> actual,
            GatewayRequestSchemaField[] declarations) {
        List<GatewayRequestSchemaField> unused = new ArrayList<>(
                List.of(declarations)
        );
        List<Binding> result = new ArrayList<>();
        for (RequestParameter parameter : actual) {
            List<GatewayRequestSchemaField> matches = unused.stream()
                    .filter(declaration -> matches(parameter, declaration))
                    .toList();
            if (matches.isEmpty()) {
                throw invalid(
                        handler,
                        "requestSchemaFields is missing "
                                + parameter.location() + " "
                                + displayName(parameter)
                );
            }
            if (matches.size() > 1) {
                throw invalid(
                        handler,
                        "duplicate requestSchemaFields declaration for "
                                + parameter.location() + " "
                                + displayName(parameter)
                );
            }
            GatewayRequestSchemaField declaration = matches.getFirst();
            validateDeclaration(handler, parameter, declaration);
            unused.remove(declaration);
            result.add(new Binding(parameter, declaration));
        }
        if (!unused.isEmpty()) {
            GatewayRequestSchemaField first = unused.getFirst();
            throw invalid(
                    handler,
                    "requestSchemaFields declares unknown "
                            + first.location() + " " + first.name()
            );
        }
        return result;
    }

    private boolean matches(
            RequestParameter parameter,
            GatewayRequestSchemaField declaration) {
        if (parameter.location() != declaration.location()) {
            return false;
        }
        if (parameter.expanded()) {
            return declaration.expanded() && declaration.name().isBlank()
                    && parameter.javaType().getRawClass()
                    .equals(declaration.schema());
        }
        if (parameter.location() == GatewayRequestLocation.BODY) {
            return declaration.name().isBlank() && !declaration.expanded();
        }
        return parameter.name().equals(declaration.name())
                && !declaration.expanded();
    }

    private void validateDeclaration(
            HandlerMethod handler,
            RequestParameter parameter,
            GatewayRequestSchemaField declaration) {
        if (declaration.location() == GatewayRequestLocation.RPC_MESSAGE) {
            throw invalid(handler, "RPC_MESSAGE is not valid for HTTP");
        }
        if (declaration.expanded()
                && (declaration.location() != GatewayRequestLocation.QUERY
                || declaration.shape() != GatewaySchemaShape.OBJECT)) {
            throw invalid(
                    handler,
                    "expanded=true requires QUERY + OBJECT"
            );
        }
        if (parameter.expanded() != declaration.expanded()) {
            throw invalid(
                    handler,
                    "ModelAttribute declaration must use expanded=true"
            );
        }
        schemaMapper.validateDeclaration(
                parameter.javaType(),
                declaration.schema(),
                declaration.shape(),
                handler.getMethod().toGenericString() + " request "
                        + parameter.location() + " "
                        + displayName(parameter)
        );
    }

    private Map<String, Object> schema(List<Binding> bindings) {
        Map<GatewayRequestLocation, Map<String, Object>> propertiesByLocation =
                new EnumMap<>(GatewayRequestLocation.class);
        Map<GatewayRequestLocation, List<String>> requiredByLocation =
                new EnumMap<>(GatewayRequestLocation.class);
        Map<String, Object> definitions = new LinkedHashMap<>();
        Set<GatewayRequestLocation> requiredLocations = new LinkedHashSet<>();

        for (Binding binding : bindings) {
            RequestParameter parameter = binding.parameter();
            Map<String, Object> generated = binding.declaration() == null
                    ? schemaMapper.schema(
                            parameter.javaType(),
                            parameter.annotatedElement()
                    )
                    : schemaMapper.declaredSchema(
                            parameter.javaType(),
                            binding.declaration().schema(),
                            binding.declaration().shape(),
                            parameter.annotatedElement(),
                            "HTTP request " + parameter.location() + " "
                                    + displayName(parameter)
                    );
            mergeDefinitions(definitions, generated);
            Map<String, Object> valueSchema = schemaBody(generated);
            if (parameter.location() == GatewayRequestLocation.BODY) {
                propertiesByLocation.put(
                        GatewayRequestLocation.BODY,
                        valueSchema
                );
                if (parameter.required()) {
                    requiredLocations.add(GatewayRequestLocation.BODY);
                }
                continue;
            }
            Map<String, Object> locationProperties = propertiesByLocation
                    .computeIfAbsent(
                            parameter.location(),
                            ignored -> new LinkedHashMap<>()
                    );
            List<String> locationRequired = requiredByLocation
                    .computeIfAbsent(
                            parameter.location(),
                            ignored -> new ArrayList<>()
                    );
            if (parameter.expanded()) {
                Map<String, Object> expanded = map(
                        valueSchema.get("properties")
                );
                expanded.forEach((name, property) -> {
                    if (locationProperties.putIfAbsent(name, property) != null) {
                        throw new IllegalArgumentException(
                                "expanded query property collision: " + name
                        );
                    }
                });
                Object required = valueSchema.get("required");
                if (required instanceof List<?> names) {
                    names.forEach(name -> locationRequired.add(
                            String.valueOf(name)
                    ));
                }
            } else {
                if (locationProperties.putIfAbsent(
                        parameter.name(),
                        valueSchema
                ) != null) {
                    throw new IllegalArgumentException(
                            "request parameter collision: " + parameter.name()
                    );
                }
                if (parameter.defaultValue() != null) {
                    valueSchema.put(
                            "default",
                            parseDefault(
                                    parameter.defaultValue(),
                                    parameter.javaType()
                            )
                    );
                }
                if (parameter.required()) {
                    locationRequired.add(parameter.name());
                }
            }
        }

        Map<String, Object> rootProperties = new LinkedHashMap<>();
        for (GatewayRequestLocation location : HTTP_LOCATIONS) {
            Map<String, Object> locationProperties = propertiesByLocation.get(
                    location
            );
            if (locationProperties == null) {
                continue;
            }
            String rootName = location.name().toLowerCase();
            if (location == GatewayRequestLocation.BODY) {
                rootProperties.put(rootName, locationProperties);
                continue;
            }
            Map<String, Object> locationSchema = new LinkedHashMap<>();
            locationSchema.put("type", "object");
            locationSchema.put("properties", locationProperties);
            List<String> required = requiredByLocation.getOrDefault(
                    location,
                    List.of()
            );
            if (!required.isEmpty()) {
                locationSchema.put("required", List.copyOf(required));
                requiredLocations.add(location);
            }
            locationSchema.put("additionalProperties", false);
            rootProperties.put(rootName, locationSchema);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("$schema", GatewayJavaSchemaMapper.JSON_SCHEMA_2020_12);
        result.put(
                "x-egon-schema-model",
                "gateway-operation-request/v2"
        );
        result.put("type", "object");
        result.put("properties", rootProperties);
        if (!requiredLocations.isEmpty()) {
            result.put(
                    "required",
                    HTTP_LOCATIONS.stream()
                            .filter(requiredLocations::contains)
                            .map(location -> location.name().toLowerCase())
                            .toList()
            );
        }
        result.put("additionalProperties", false);
        if (!definitions.isEmpty()) {
            result.put("$defs", definitions);
        }
        return result;
    }

    private List<RequestParameter> parameters(
            HandlerMethod handler,
            String routePath) {
        List<RequestParameter> result = new ArrayList<>();
        for (MethodParameter methodParameter : handler.getMethodParameters()) {
            if (frameworkType(methodParameter.getParameterType())) {
                continue;
            }
            GatewayRequestLocation location = location(methodParameter);
            boolean expanded = expanded(methodParameter, location);
            String name = expanded || location == GatewayRequestLocation.BODY
                    ? "" : name(methodParameter);
            if (location == GatewayRequestLocation.PATH
                    && !routePath.matches(
                    ".*\\{" + java.util.regex.Pattern.quote(name)
                            + "(?:[:}]).*")) {
                throw invalid(
                        handler,
                        "PATH parameter is absent from route template: " + name
                );
            }
            boolean required = required(methodParameter, location);
            GatewaySchemaField field = methodParameter.getParameter()
                    .getAnnotation(GatewaySchemaField.class);
            if (field != null) {
                if (field.required() == GatewaySchemaRequired.OPTIONAL
                        && required) {
                    throw invalid(
                            handler,
                            "GatewaySchemaField OPTIONAL conflicts with required "
                                    + location + " " + name
                    );
                }
                if (field.required() == GatewaySchemaRequired.REQUIRED) {
                    required = true;
                }
            }
            result.add(new RequestParameter(
                    location,
                    name,
                    required,
                    expanded,
                    objectMapper.constructType(
                            methodParameter.getGenericParameterType()
                    ),
                    methodParameter.getParameter(),
                    defaultValue(methodParameter)
            ));
        }
        return result;
    }

    private GatewayRequestLocation location(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(PathVariable.class)) {
            return GatewayRequestLocation.PATH;
        }
        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            return GatewayRequestLocation.QUERY;
        }
        if (parameter.hasParameterAnnotation(RequestHeader.class)) {
            return GatewayRequestLocation.HEADER;
        }
        if (parameter.hasParameterAnnotation(CookieValue.class)) {
            return GatewayRequestLocation.COOKIE;
        }
        if (parameter.hasParameterAnnotation(RequestPart.class)) {
            return GatewayRequestLocation.PART;
        }
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return GatewayRequestLocation.BODY;
        }
        return GatewayRequestLocation.QUERY;
    }

    private boolean expanded(
            MethodParameter parameter,
            GatewayRequestLocation location) {
        if (location != GatewayRequestLocation.QUERY
                || parameter.hasParameterAnnotation(RequestParam.class)) {
            return false;
        }
        return parameter.hasParameterAnnotation(ModelAttribute.class)
                || !simple(parameter.getParameterType());
    }

    private String name(MethodParameter parameter) {
        PathVariable path = parameter.getParameterAnnotation(
                PathVariable.class
        );
        if (path != null) {
            return annotationName(path.name(), path.value(), parameter);
        }
        RequestParam query = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (query != null) {
            return annotationName(query.name(), query.value(), parameter);
        }
        RequestHeader header = parameter.getParameterAnnotation(
                RequestHeader.class
        );
        if (header != null) {
            return annotationName(header.name(), header.value(), parameter);
        }
        CookieValue cookie = parameter.getParameterAnnotation(
                CookieValue.class
        );
        if (cookie != null) {
            return annotationName(cookie.name(), cookie.value(), parameter);
        }
        RequestPart part = parameter.getParameterAnnotation(
                RequestPart.class
        );
        if (part != null) {
            return annotationName(part.name(), part.value(), parameter);
        }
        return parameterName(parameter);
    }

    private String annotationName(
            String name,
            String value,
            MethodParameter parameter) {
        if (!name.isBlank()) {
            return name;
        }
        if (!value.isBlank()) {
            return value;
        }
        return parameterName(parameter);
    }

    private String parameterName(MethodParameter parameter) {
        return parameter.getParameterName() == null
                ? "arg" + parameter.getParameterIndex()
                : parameter.getParameterName();
    }

    private boolean required(
            MethodParameter parameter,
            GatewayRequestLocation location) {
        boolean constrained = parameter.hasParameterAnnotation(NotNull.class)
                || parameter.hasParameterAnnotation(NotBlank.class)
                || parameter.hasParameterAnnotation(NotEmpty.class);
        if (location == GatewayRequestLocation.PATH) {
            return true;
        }
        RequestParam query = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (query != null) {
            return constrained || query.required()
                    && ValueConstants.DEFAULT_NONE.equals(query.defaultValue());
        }
        RequestHeader header = parameter.getParameterAnnotation(
                RequestHeader.class
        );
        if (header != null) {
            return constrained || header.required()
                    && ValueConstants.DEFAULT_NONE.equals(header.defaultValue());
        }
        CookieValue cookie = parameter.getParameterAnnotation(
                CookieValue.class
        );
        if (cookie != null) {
            return constrained || cookie.required()
                    && ValueConstants.DEFAULT_NONE.equals(cookie.defaultValue());
        }
        RequestPart part = parameter.getParameterAnnotation(
                RequestPart.class
        );
        if (part != null) {
            return constrained || part.required();
        }
        RequestBody body = parameter.getParameterAnnotation(
                RequestBody.class
        );
        return constrained || body != null && body.required();
    }

    private String defaultValue(MethodParameter parameter) {
        RequestParam query = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (query != null
                && !ValueConstants.DEFAULT_NONE.equals(query.defaultValue())) {
            return query.defaultValue();
        }
        RequestHeader header = parameter.getParameterAnnotation(
                RequestHeader.class
        );
        if (header != null
                && !ValueConstants.DEFAULT_NONE.equals(header.defaultValue())) {
            return header.defaultValue();
        }
        CookieValue cookie = parameter.getParameterAnnotation(
                CookieValue.class
        );
        if (cookie != null
                && !ValueConstants.DEFAULT_NONE.equals(cookie.defaultValue())) {
            return cookie.defaultValue();
        }
        return null;
    }

    private Object parseDefault(String value, JavaType type) {
        Class<?> raw = type.getRawClass();
        if (raw == boolean.class || raw == Boolean.class) {
            return Boolean.valueOf(value);
        }
        if (raw == byte.class || raw == Byte.class
                || raw == short.class || raw == Short.class
                || raw == int.class || raw == Integer.class) {
            return Integer.valueOf(value);
        }
        if (raw == long.class || raw == Long.class) {
            return Long.valueOf(value);
        }
        if (raw == float.class || raw == Float.class
                || raw == double.class || raw == Double.class) {
            return Double.valueOf(value);
        }
        return value;
    }

    private boolean simple(Class<?> type) {
        return type.isPrimitive() || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class || type == java.util.UUID.class
                || type.getName().startsWith("java.time.");
    }

    private boolean frameworkType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("jakarta.servlet.")
                || name.startsWith("javax.servlet.")
                || name.startsWith("org.springframework.http.server.")
                || name.startsWith("org.springframework.web.server.")
                || java.security.Principal.class.isAssignableFrom(type);
    }

    private IllegalArgumentException invalid(
            HandlerMethod handler,
            String message) {
        return new IllegalArgumentException(
                "invalid Gateway request schema for "
                        + handler.getMethod().toGenericString() + ": " + message
        );
    }

    private String displayName(RequestParameter parameter) {
        return parameter.name().isBlank() ? "<root>" : parameter.name();
    }

    private void mergeDefinitions(
            Map<String, Object> target,
            Map<String, Object> schema) {
        Object value = schema.get("$defs");
        if (!(value instanceof Map<?, ?> source)) {
            return;
        }
        source.forEach((key, definition) -> {
            Object previous = target.putIfAbsent(
                    String.valueOf(key),
                    definition
            );
            if (previous != null && !previous.equals(definition)) {
                throw new IllegalArgumentException(
                        "conflicting Java schema definition: " + key
                );
            }
        });
    }

    private Map<String, Object> schemaBody(Map<String, Object> generated) {
        Map<String, Object> result = new LinkedHashMap<>(generated);
        result.remove("$schema");
        result.remove("$defs");
        result.remove("x-egon-schema-model");
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?>
                ? (Map<String, Object>) value : Map.of();
    }

    record RequestParameter(
            GatewayRequestLocation location,
            String name,
            boolean required,
            boolean expanded,
            JavaType javaType,
            AnnotatedElement annotatedElement,
            String defaultValue
    ) {
    }

    record Result(
            Map<String, Object> schema,
            List<RequestParameter> parameters
    ) {
    }

    private record Binding(
            RequestParameter parameter,
            GatewayRequestSchemaField declaration
    ) {
    }
}
