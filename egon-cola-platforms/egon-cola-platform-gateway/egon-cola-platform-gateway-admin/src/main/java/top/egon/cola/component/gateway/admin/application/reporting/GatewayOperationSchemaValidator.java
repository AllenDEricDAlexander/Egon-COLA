package top.egon.cola.component.gateway.admin.application.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the v2 operation schemas received from a Gateway starter.
 *
 * <p>The report is the source of truth for both ordinary Gateway operations
 * and declarative MCP tools, so this validator deliberately rejects schema
 * shapes that cannot be interpreted by the Gateway runtime.
 */
public final class GatewayOperationSchemaValidator {

    private static final String REQUEST_MODEL =
            "gateway-operation-request/v2";

    private static final String RESPONSE_MODEL =
            "gateway-operation-response/v2";

    private static final Set<String> HTTP_LOCATIONS = Set.of(
            "path", "query", "header", "cookie", "body", "part"
    );

    private static final int MAX_DEPTH = 32;

    private static final int MAX_NODES = 10_000;

    private static final int MAX_SERIALIZED_BYTES = 2 * 1024 * 1024;

    private final ObjectMapper objectMapper;

    public GatewayOperationSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(
            String operationKey,
            String protocol,
            Map<String, Object> requestSchema,
            Map<String, Object> responseSchema,
            Map<String, Object> attributes) {
        String identity = operationKey == null || operationKey.isBlank()
                ? "operation"
                : operationKey;
        requireSchema(identity + ".requestSchema", requestSchema, REQUEST_MODEL);
        requireSchema(identity + ".responseSchema", responseSchema, RESPONSE_MODEL);
        boolean mcp = registeredForMcp(attributes);
        validateNode(identity + ".requestSchema", requestSchema, requestSchema,
                protocol, mcp, new State());
        validateNode(identity + ".responseSchema", responseSchema, responseSchema,
                protocol, false, new State());
        if ("HTTP".equalsIgnoreCase(protocol)) {
            validateHttpRoot(identity, requestSchema, mcp);
        } else if ("RPC".equalsIgnoreCase(protocol)) {
            validateObjectRoot(identity + ".requestSchema", requestSchema);
            validateObjectRoot(identity + ".responseSchema", responseSchema);
        } else {
            throw invalid(identity, "unsupported operation protocol: " + protocol);
        }
        if (mcp && Boolean.TRUE.equals(attributes.get("streaming"))) {
            throw invalid(identity, "streaming operations are unsupported");
        }
    }

    public void validate(GatewayInterfaceDefinitionReport.Operation operation) {
        validate(
                operation.operationKey(),
                operation.protocol(),
                operation.requestSchema(),
                operation.responseSchema(),
                operation.attributes()
        );
    }

    private void requireSchema(
            String identity,
            Map<String, Object> schema,
            String expectedModel) {
        if (schema == null || schema.isEmpty()) {
            throw invalid(identity, "schema is required");
        }
        Object model = schema.get("x-egon-schema-model");
        if (!expectedModel.equals(model)) {
            throw invalid(identity, "schema model must be " + expectedModel);
        }
        try {
            if (objectMapper.writeValueAsBytes(schema).length
                    > MAX_SERIALIZED_BYTES) {
                throw invalid(identity, "schema exceeds serialized size limit");
            }
        } catch (JsonProcessingException failure) {
            throw invalid(identity, "schema is not serializable", failure);
        }
    }

    private void validateHttpRoot(
            String identity,
            Map<String, Object> schema,
            boolean mcp) {
        validateObjectRoot(identity + ".requestSchema", schema);
        Map<String, Object> properties = map(schema.get("properties"));
        if (properties == null) {
            throw invalid(identity, "HTTP request properties are required");
        }
        for (String location : properties.keySet()) {
            if (!HTTP_LOCATIONS.contains(location)) {
                throw invalid(identity, "unknown HTTP location group: " + location);
            }
            Object value = properties.get(location);
            if (!(value instanceof Map<?, ?>)) {
                throw invalid(identity, "HTTP location group must be an object: " + location);
            }
            if (!"body".equals(location)) {
                validateObjectRoot(identity + ".requestSchema." + location,
                        cast(value));
            }
            if (mcp) {
                validateMcpLocation(identity, location, cast(value));
            }
        }
        validateRequired(identity + ".requestSchema", schema, properties.keySet());
    }

    private void validateMcpLocation(
            String identity,
            String location,
            Map<String, Object> schema) {
        if ("part".equals(location)) {
            throw invalid(identity, "PART parameters are unsupported");
        }
        if (!"header".equals(location) && !"cookie".equals(location)) {
            return;
        }
        Map<String, Object> properties = map(schema.get("properties"));
        if (properties == null) {
            return;
        }
        Set<String> required = requiredNames(identity + "." + location, schema,
                properties.keySet());
        for (String name : required) {
            if (!("header".equals(location)
                    && "authorization".equalsIgnoreCase(name))) {
                throw invalid(identity, "required " + location.toUpperCase()
                        + " parameter is unsupported: " + name);
            }
        }
    }

    private void validateNode(
            String identity,
            Object value,
            Map<String, Object> root,
            String protocol,
            boolean mcp,
            State state) {
        if (!(value instanceof Map<?, ?> raw)) {
            return;
        }
        if (++state.nodes > MAX_NODES) {
            throw invalid(identity, "schema node limit exceeded");
        }
        if (state.depth > MAX_DEPTH) {
            throw invalid(identity, "schema depth limit exceeded");
        }
        Map<String, Object> node = cast(raw);
        Object reference = node.get("$ref");
        if (reference != null) {
            if (!(reference instanceof String ref)
                    || !ref.startsWith("#/$defs/")) {
                throw invalid(identity, "external $ref is not allowed");
            }
            String key = ref.substring("#/$defs/".length());
            Map<String, Object> definitions = map(root.get("$defs"));
            if (definitions == null || key.isBlank() || !definitions.containsKey(key)) {
                throw invalid(identity, "unresolved local $ref: " + ref);
            }
        }
        if (node.containsKey("required")) {
            Map<String, Object> properties = map(node.get("properties"));
            if (properties == null) {
                throw invalid(identity, "required requires properties");
            }
            requiredNames(identity, node, properties.keySet());
        }
        Map<String, Object> properties = map(node.get("properties"));
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                validateNode(identity + ".properties." + entry.getKey(),
                        entry.getValue(), root, protocol, mcp,
                        state.child());
            }
        }
        validateChild(identity, "items", node.get("items"), root, protocol, mcp,
                state);
        validateChild(identity, "additionalProperties",
                node.get("additionalProperties"), root, protocol, mcp, state);
        for (String keyword : List.of("anyOf", "oneOf", "allOf", "prefixItems")) {
            Object children = node.get(keyword);
            if (children instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    validateNode(identity + "." + keyword + "[" + i + "]",
                            list.get(i), root, protocol, mcp, state.child());
                }
            } else if (children != null) {
                throw invalid(identity, keyword + " must be an array");
            }
        }
        if (node.get("$defs") instanceof Map<?, ?> definitions) {
            for (Map.Entry<?, ?> entry : definitions.entrySet()) {
                validateNode(identity + ".$defs." + entry.getKey(), entry.getValue(),
                        root, protocol, mcp, state.child());
            }
        }
    }

    private void validateChild(
            String identity,
            String keyword,
            Object child,
            Map<String, Object> root,
            String protocol,
            boolean mcp,
            State state) {
        if (child instanceof Boolean) {
            return;
        }
        if (child != null && !(child instanceof Map<?, ?>)) {
            throw invalid(identity, keyword + " must be a schema");
        }
        validateNode(identity + "." + keyword, child, root, protocol, mcp,
                state.child());
    }

    private void validateObjectRoot(String identity, Map<String, Object> schema) {
        if (!"object".equals(schema.get("type"))) {
            throw invalid(identity, "schema root must be an object");
        }
        if (map(schema.get("properties")) == null) {
            throw invalid(identity, "schema properties are required");
        }
    }

    private Set<String> requiredNames(
            String identity,
            Map<String, Object> schema,
            Set<String> propertyNames) {
        Object value = schema.get("required");
        if (!(value instanceof List<?> required)) {
            throw invalid(identity, "required must be an array of strings");
        }
        Set<String> result = new HashSet<>();
        for (Object entry : required) {
            if (!(entry instanceof String name) || name.isBlank()
                    || !propertyNames.contains(name) || !result.add(name)) {
                throw invalid(identity, "required contains an unknown or duplicate property");
            }
        }
        return result;
    }

    private void validateRequired(
            String identity,
            Map<String, Object> schema,
            Set<String> propertyNames) {
        if (schema.containsKey("required")) {
            requiredNames(identity, schema, propertyNames);
        }
    }

    private boolean registeredForMcp(Map<String, Object> attributes) {
        if (attributes == null) {
            return false;
        }
        Object value = attributes.get("mcpExposure");
        return value instanceof Map<?, ?> exposure
                && Boolean.TRUE.equals(exposure.get("registerMcp"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?>
                ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    private IllegalArgumentException invalid(String identity, String message) {
        return new IllegalArgumentException(identity + ": " + message);
    }

    private IllegalArgumentException invalid(
            String identity,
            String message,
            Throwable cause) {
        return new IllegalArgumentException(identity + ": " + message, cause);
    }

    private static final class State {
        private final int depth;
        private int nodes;

        private State() {
            this(0);
        }

        private State(int depth) {
            this.depth = depth;
        }

        private State child() {
            State child = new State(depth + 1);
            child.nodes = nodes;
            return child;
        }
    }
}
