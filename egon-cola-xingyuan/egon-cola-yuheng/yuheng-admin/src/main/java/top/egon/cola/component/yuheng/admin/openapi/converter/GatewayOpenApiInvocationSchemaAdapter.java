package top.egon.cola.component.yuheng.admin.openapi.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Converts OpenAPI parameter, request-body and response schemas to Invocation
 * Schema v2 maps.
 *
 * <p>中文：该类只负责 OpenAPI graph traversal；不会访问网络或持久化，且
 * 将 OpenAPI component refs 映射到 Invocation v2 的本地 {@code $defs}。</p>
 */
@Slf4j
@Component("gatewayOpenApiInvocationSchemaAdapter")
public class GatewayOpenApiInvocationSchemaAdapter {

    public static final String REQUEST_SCHEMA_MODEL =
            "yuheng-operation-request/v2";

    public static final String RESPONSE_SCHEMA_MODEL =
            "yuheng-operation-response/v2";

    private static final String JSON_SCHEMA =
            "https://json-schema.org/draft/2020-12/schema";

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "options", "head", "patch",
            "trace"
    );

    private static final Set<String> PARAMETER_LOCATIONS = Set.of(
            "path", "query", "header", "cookie"
    );

    private static final int MAX_SCHEMA_DEPTH = 64;

    private static final int MAX_SCHEMA_NODES = 50_000;

    private final ObjectMapper objectMapper;

    /** Creates an adapter using a deterministic Jackson mapper. */
    public GatewayOpenApiInvocationSchemaAdapter() {
        this(deterministicMapper());
    }

    /** Creates an adapter with the application Jackson mapper. */
    public GatewayOpenApiInvocationSchemaAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Builds the Invocation request schema for one OpenAPI operation.
     *
     * @param root complete OpenAPI document
     * @param pathItem path item containing path-level parameters
     * @param operation operation containing operation-level parameters/body
     * @return normalized request schema
     */
    public synchronized Map<String, Object> requestSchema(
            JsonNode root,
            JsonNode pathItem,
            JsonNode operation) {
        schemaNodes = 0;
        requireObject(root, "OpenAPI root");
        requireObject(pathItem, "OpenAPI path item");
        requireObject(operation, "OpenAPI operation");

        Map<ParameterKey, JsonNode> parameters = new LinkedHashMap<>();
        collectParameters(root, pathItem.get("parameters"), parameters, false);
        collectParameters(root, operation.get("parameters"), parameters, true);

        Map<String, Map<String, Object>> locationProperties =
                new TreeMap<>();
        Map<String, List<String>> locationRequired = new TreeMap<>();
        for (Map.Entry<ParameterKey, JsonNode> entry : parameters.entrySet()) {
            ParameterKey key = entry.getKey();
            JsonNode parameter = resolveParameter(root, entry.getValue());
            String location = key.location();
            Map<String, Object> properties = locationProperties.computeIfAbsent(
                    location,
                    ignored -> new TreeMap<>()
            );
            Map<String, Object> parameterDefinition = schema(
                    root, parameterSchema(root, parameter), 0);
            copyDescription(parameter, parameterDefinition);
            properties.put(key.name(), parameterDefinition);
            boolean required = "path".equals(location)
                    || parameter.path("required").asBoolean(false);
            if (required) {
                locationRequired.computeIfAbsent(
                        location,
                        ignored -> new ArrayList<>()
                ).add(key.name());
            }
        }

        Map<String, Object> properties = new TreeMap<>();
        List<String> requiredLocations = new ArrayList<>();
        for (String location : List.of("path", "query", "header", "cookie")) {
            if (!locationProperties.containsKey(location)) {
                continue;
            }
            List<String> required = locationRequired.getOrDefault(
                    location,
                    List.of()
            );
            properties.put(
                    location,
                    groupedLocation(
                            location,
                            locationProperties.get(location),
                            required
                    )
            );
            if (!required.isEmpty()) {
                requiredLocations.add(location);
            }
        }

        JsonNode requestBody = resolveRequestBody(root, operation.get("requestBody"));
        Map<String, Object> inlineDefinitions = new TreeMap<>();
        if (requestBody != null) {
            MediaSelection selection = selectMedia(
                    root,
                    requestBody.path("content"),
                    "request body"
            );
            if (selection != null) {
                String location = selection.mediaType().toLowerCase(Locale.ROOT)
                        .startsWith("multipart/") ? "part" : "body";
                Map<String, Object> bodySchema = schemaWithModel(
                                root,
                                selection.schema(),
                                REQUEST_SCHEMA_MODEL
                        );
                extractDefinitions(bodySchema, inlineDefinitions);
                copyDescription(requestBody, bodySchema);
                properties.put(location, bodySchema);
                if (requestBody.path("required").asBoolean(false)) {
                    requiredLocations.add(location);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("$schema", JSON_SCHEMA);
        result.put("x-egon-schema-model", REQUEST_SCHEMA_MODEL);
        result.put("type", "object");
        result.put("properties", properties);
        result.put("required", requiredLocations.stream().distinct().sorted().toList());
        result.put("additionalProperties", false);
        mergeDefinitions(result, inlineDefinitions);
        appendDefinitions(root, result);
        return result;
    }

    /** Alias with an algorithm-oriented name for callers that prefer it. */
    public Map<String, Object> adaptRequestSchema(
            JsonNode root,
            JsonNode pathItem,
            JsonNode operation) {
        return requestSchema(root, pathItem, operation);
    }

    /**
     * Selects and maps the primary response according to the stable 200,
     * lowest-2xx, default priority.
     */
    public synchronized Map<String, Object> responseSchema(
            JsonNode root,
            JsonNode operation) {
        schemaNodes = 0;
        ResponseSelection selected = selectedResponse(root, operation);
        JsonNode response = selected.response();
        MediaSelection media = selectMedia(
                root,
                response.path("content"),
                "response " + selected.status()
        );
        Map<String, Object> result;
        if (media == null || "204".equals(selected.status())) {
            result = new LinkedHashMap<>();
            result.put("type", "null");
        } else {
            result = schema(root, media.schema(), 0);
        }
        Map<String, Object> inlineDefinitions = new TreeMap<>();
        extractDefinitions(result, inlineDefinitions);
        result.put("x-egon-schema-model", RESPONSE_SCHEMA_MODEL);
        result.putIfAbsent("$schema", JSON_SCHEMA);
        mergeDefinitions(result, inlineDefinitions);
        appendDefinitions(root, result);
        return result;
    }

    /** Alias for the primary response mapping. */
    public Map<String, Object> adaptResponseSchema(
            JsonNode root,
            JsonNode operation) {
        return responseSchema(root, operation);
    }

    /**
     * Maps all non-selected responses into deterministic error-schema entries.
     */
    public synchronized List<Map<String, Object>> errorSchemas(
            JsonNode root,
            JsonNode operation) {
        schemaNodes = 0;
        requireObject(root, "OpenAPI root");
        requireObject(operation, "OpenAPI operation");
        ResponseSelection selected = selectedResponse(root, operation);
        JsonNode responses = operation.path("responses");
        List<String> statuses = new ArrayList<>();
        responses.fieldNames().forEachRemaining(statuses::add);
        statuses.remove(selected.status());
        statuses.sort(this::compareResponseStatus);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String status : statuses) {
            JsonNode response = responseNode(root, responses, status);
            if (response == null || !response.isObject()) {
                throw invalid("response " + status + " must be an object");
            }
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", status);
            error.put("description", text(response, "description", ""));
            MediaSelection media = selectMedia(
                    root,
                    response.path("content"),
                    "response " + status
            );
            error.put(
                    "contentTypes",
                    mediaTypes(response.path("content"))
            );
            if (media == null || "204".equals(status)) {
                error.put("schema", Map.of(
                        "type", "null",
                        "x-egon-schema-model", RESPONSE_SCHEMA_MODEL
                ));
            } else {
                Map<String, Object> errorSchema = schemaWithModel(
                        root, media.schema(), RESPONSE_SCHEMA_MODEL);
                appendDefinitions(root, errorSchema);
                error.put("schema", errorSchema);
            }
            result.add(Map.copyOf(error));
        }
        return List.copyOf(result);
    }

    /** Returns all request-body media types in sorted order. */
    public List<String> requestMediaTypes(JsonNode operation) {
        JsonNode body = operation == null ? null : operation.get("requestBody");
        JsonNode content = body == null ? null : body.path("content");
        return mediaTypes(content);
    }

    /** Returns request media types after resolving a local requestBody ref. */
    public List<String> requestMediaTypes(JsonNode root, JsonNode operation) {
        JsonNode body = operation == null ? null : operation.get("requestBody");
        JsonNode resolved = resolveRequestBody(root, body);
        return mediaTypes(resolved == null ? null : resolved.path("content"));
    }

    /** Returns all response media types in sorted order. */
    public List<String> responseMediaTypes(JsonNode operation) {
        JsonNode responses = operation == null ? null : operation.path("responses");
        if (responses == null || !responses.isObject()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        responses.elements().forEachRemaining(response -> values.addAll(
                mediaTypes(response.path("content"))
        ));
        return values.stream().distinct().sorted().toList();
    }

    /** Returns response media types after resolving local response refs. */
    public List<String> responseMediaTypes(JsonNode root, JsonNode operation) {
        JsonNode responses = operation == null ? null : operation.path("responses");
        if (responses == null || !responses.isObject()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        responses.fieldNames().forEachRemaining(status -> values.addAll(
                mediaTypes(responseNode(root, responses, status).path("content"))
        ));
        return values.stream().distinct().sorted().toList();
    }

    /** Returns whether any response media type is a supported streaming type. */
    public boolean streaming(JsonNode operation) {
        return responseMediaTypes(operation).stream().anyMatch(
                value -> "text/event-stream".equalsIgnoreCase(value)
                        || "application/x-ndjson".equalsIgnoreCase(value)
        );
    }

    /** Returns streaming state after resolving local response refs. */
    public boolean streaming(JsonNode root, JsonNode operation) {
        return responseMediaTypes(root, operation).stream().anyMatch(
                value -> "text/event-stream".equalsIgnoreCase(value)
                        || "application/x-ndjson".equalsIgnoreCase(value)
        );
    }

    private void collectParameters(
            JsonNode root,
            JsonNode values,
            Map<ParameterKey, JsonNode> target,
            boolean operationLevel) {
        if (values == null || values.isMissingNode() || values.isNull()) {
            return;
        }
        if (!values.isArray()) {
            throw invalid("parameters must be an array");
        }
        Set<ParameterKey> local = new java.util.HashSet<>();
        Iterator<JsonNode> iterator = values.elements();
        while (iterator.hasNext()) {
            JsonNode parameter = resolveParameter(root, iterator.next());
            if (!parameter.isObject()) {
                throw invalid("parameter must be an object");
            }
            String location = text(parameter, "in", null);
            String name = text(parameter, "name", null);
            if (!PARAMETER_LOCATIONS.contains(location) || name == null) {
                throw invalid("parameter must declare a supported in/name pair");
            }
            ParameterKey key = new ParameterKey(location, name);
            if (!local.add(key)) {
                throw invalid("duplicate parameter " + location + ":" + name);
            }
            if (operationLevel) {
                target.put(key, parameter);
            } else {
                target.putIfAbsent(key, parameter);
            }
        }
    }

    private JsonNode resolveParameter(JsonNode root, JsonNode parameter) {
        if (parameter == null || parameter.isMissingNode() || parameter.isNull()) {
            throw invalid("parameter is required");
        }
        if (!parameter.isObject()) {
            throw invalid("parameter must be an object");
        }
        JsonNode reference = parameter.get("$ref");
        if (reference == null) {
            return parameter;
        }
        if (!reference.isTextual()
                || !reference.asText().startsWith("#/components/parameters/")) {
            throw invalid("parameter references must remain local");
        }
        JsonNode resolved = root.at(reference.asText().substring(1));
        if (!resolved.isObject()) {
            throw invalid("parameter reference target was not found");
        }
        return referenceDocumentation(resolved, parameter);
    }

    private JsonNode resolveRequestBody(JsonNode root, JsonNode requestBody) {
        if (requestBody == null || requestBody.isMissingNode()
                || requestBody.isNull()) {
            return null;
        }
        if (!requestBody.isObject()) {
            throw invalid("requestBody must be an object");
        }
        JsonNode reference = requestBody.get("$ref");
        if (reference == null) {
            return requestBody;
        }
        if (!reference.isTextual()
                || !reference.asText().startsWith("#/components/requestBodies/")) {
            throw invalid("requestBody references must remain local");
        }
        JsonNode resolved = root.at(reference.asText().substring(1));
        if (!resolved.isObject()) {
            throw invalid("requestBody reference target was not found");
        }
        return referenceDocumentation(resolved, requestBody);
    }

    /** Carries the Parameter/Request Body description into its displayed schema. */
    private void copyDescription(JsonNode source, Map<String, Object> target) {
        JsonNode description = source.get("description");
        if (description != null && description.isTextual()) {
            target.put("description", description.asText());
        }
    }

    /** OpenAPI 3.1 Reference Object documentation overrides its referenced object. */
    private JsonNode referenceDocumentation(JsonNode target, JsonNode reference) {
        ObjectNode resolved = target.deepCopy();
        for (String name : List.of("description", "summary")) {
            JsonNode value = reference.get(name);
            if (value != null && value.isTextual()) {
                resolved.set(name, value);
            }
        }
        return resolved;
    }

    private JsonNode parameterSchema(JsonNode root, JsonNode parameter) {
        JsonNode schema = parameter.get("schema");
        if (schema != null) {
            return schema;
        }
        JsonNode content = parameter.path("content");
        MediaSelection selection = selectMedia(root, content, "parameter");
        return selection == null ? objectMapper.createObjectNode() : selection.schema();
    }

    private Map<String, Object> groupedLocation(
            String location,
            Map<String, Object> properties,
            List<String> required) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "object");
        result.put("description", switch (location) {
            case "path" -> "路径参数";
            case "query" -> "查询参数";
            case "header" -> "请求头参数";
            case "cookie" -> "Cookie 参数";
            default -> throw invalid("unsupported parameter location");
        });
        result.put("properties", new TreeMap<>(properties));
        result.put("required", required.stream().distinct().sorted().toList());
        result.put("additionalProperties", false);
        return result;
    }

    private Map<String, Object> schemaWithModel(
            JsonNode root,
            JsonNode value,
            String model) {
        Map<String, Object> schema = schema(root, value, 0);
        schema.put("x-egon-schema-model", model);
        return schema;
    }

    @SuppressWarnings("unchecked")
    private void extractDefinitions(
            Map<String, Object> schema,
            Map<String, Object> target) {
        Object definitions = schema.remove("$defs");
        if (definitions instanceof Map<?, ?> values) {
            values.forEach((key, value) -> target.put(
                    String.valueOf(key),
                    value
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeDefinitions(
            Map<String, Object> target,
            Map<String, Object> inlineDefinitions) {
        if (inlineDefinitions.isEmpty()) {
            return;
        }
        Map<String, Object> merged = new TreeMap<>();
        Object existing = target.get("$defs");
        if (existing instanceof Map<?, ?> values) {
            values.forEach((key, value) -> merged.put(
                    String.valueOf(key),
                    value
            ));
        }
        inlineDefinitions.forEach(merged::putIfAbsent);
        target.put("$defs", merged);
    }

    private Map<String, Object> schema(JsonNode root, JsonNode value, int depth) {
        if (depth > MAX_SCHEMA_DEPTH) {
            throw invalid("schema depth exceeds the supported limit");
        }
        if (++schemaNodes > MAX_SCHEMA_NODES) {
            throw invalid("schema node count exceeds the supported limit");
        }
        if (value == null || value.isMissingNode() || value.isNull()) {
            return new LinkedHashMap<>();
        }
        if (value.isBoolean()) {
            return value.asBoolean() ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(Map.of("not", Map.of()));
        }
        if (!value.isObject()) {
            throw invalid("schema must be an object or boolean");
        }
        JsonNode reference = value.get("$ref");
        if (reference != null) {
            if (!reference.isTextual()
                    || !reference.asText().startsWith("#/components/schemas/")) {
                throw invalid("schema references must remain local");
            }
            String name = reference.asText().substring(
                    "#/components/schemas/".length()
            );
            if (name.isBlank()) {
                throw invalid("schema reference name is required");
            }
            if (root.at(reference.asText().substring(1)).isMissingNode()) {
                throw invalid("schema reference target was not found");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (reference != null) {
            // The component name is already a JSON Pointer token; do not escape it twice.
            result.put("$ref", "#/$defs/" + reference.asText().substring(
                    "#/components/schemas/".length()));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode child = field.getValue();
            if ("$ref".equals(name)) {
                continue;
            }
            if (Set.of("properties", "patternProperties", "dependentSchemas", "$defs")
                    .contains(name) && child.isObject()) {
                Map<String, Object> properties = new TreeMap<>();
                child.fields().forEachRemaining(entry -> properties.put(
                        entry.getKey(),
                        schema(root, entry.getValue(), depth + 1)
                ));
                result.put(name, properties);
            } else if (Set.of("items", "additionalProperties", "not", "contains",
                    "propertyNames", "if", "then", "else", "unevaluatedProperties",
                    "unevaluatedItems", "contentSchema").contains(name)) {
                result.put(name, child.isBoolean()
                        ? child.asBoolean()
                        : schema(root, child, depth + 1));
            } else if (Set.of("allOf", "anyOf", "oneOf", "prefixItems")
                    .contains(name) && child.isArray()) {
                List<Object> children = new ArrayList<>();
                child.elements().forEachRemaining(item -> children.add(
                        schema(root, item, depth + 1)
                ));
                result.put(name, children);
            } else if (("required".equals(name) || "enum".equals(name))
                    && child.isArray()) {
                List<Object> values = new ArrayList<>();
                child.elements().forEachRemaining(item -> values.add(
                        objectMapper.convertValue(item, Object.class)
                ));
                values.sort(Comparator.comparing(String::valueOf));
                result.put(name, values);
            } else {
                result.put(name, objectMapper.convertValue(child, Object.class));
            }
        }
        return result;
    }

    private void appendDefinitions(JsonNode root, Map<String, Object> target) {
        JsonNode schemas = root.path("components").path("schemas");
        if (!schemas.isObject() || schemas.isEmpty()) {
            return;
        }
        Map<String, Object> definitions = new TreeMap<>();
        Set<String> referenced = new LinkedHashSet<>();
        collectDefinitionReferences(target, referenced);
        ArrayDeque<String> pending = new ArrayDeque<>(referenced);
        // Keep the complete reachable graph, including cycles, without copying unrelated schemas.
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            JsonNode source = schemas.get(name);
            if (source == null) {
                continue;
            }
            Map<String, Object> definition = schema(root, source, 0);
            definitions.put(name, definition);
            Set<String> dependencies = new LinkedHashSet<>();
            collectDefinitionReferences(definition, dependencies);
            for (String dependency : dependencies) {
                if (referenced.add(dependency)) {
                    pending.addLast(dependency);
                }
            }
        }
        mergeDefinitions(target, definitions);
    }

    private void collectDefinitionReferences(Object value, Set<String> references) {
        if (!(value instanceof Map<?, ?> schema)) {
            return;
        }
        if (schema.get("$ref") instanceof String ref && ref.startsWith("#/$defs/")) {
            String token = ref.substring("#/$defs/".length()).split("/", 2)[0];
            references.add(token.replace("~1", "/").replace("~0", "~"));
        }
        schema.forEach((name, child) -> {
            if (Set.of("properties", "patternProperties", "dependentSchemas", "$defs")
                    .contains(name) && child instanceof Map<?, ?> properties) {
                properties.values().forEach(property -> collectDefinitionReferences(property, references));
            } else if (Set.of("items", "additionalProperties", "not", "contains",
                    "propertyNames", "if", "then", "else", "unevaluatedProperties",
                    "unevaluatedItems", "contentSchema").contains(name)) {
                collectDefinitionReferences(child, references);
            } else if (Set.of("allOf", "anyOf", "oneOf", "prefixItems").contains(name)
                    && child instanceof List<?> items) {
                items.forEach(item -> collectDefinitionReferences(item, references));
            }
        });
    }

    private ResponseSelection selectedResponse(JsonNode root, JsonNode operation) {
        requireObject(root, "OpenAPI root");
        requireObject(operation, "OpenAPI operation");
        JsonNode responses = operation.get("responses");
        if (responses == null || !responses.isObject() || responses.isEmpty()) {
            throw invalid("responses must contain at least one response");
        }
        if (responses.has("200")) {
            return new ResponseSelection("200", responseNode(root, responses, "200"));
        }
        List<Integer> success = new ArrayList<>();
        responses.fieldNames().forEachRemaining(status -> {
            try {
                int code = Integer.parseInt(status);
                if (code >= 200 && code < 300) {
                    success.add(code);
                }
            } catch (NumberFormatException ignored) {
                // default and non-numeric response keys are not successful.
            }
        });
        if (!success.isEmpty()) {
            String status = String.valueOf(success.stream().min(Integer::compareTo)
                    .orElseThrow());
            return new ResponseSelection(status, responseNode(root, responses, status));
        }
        if (responses.has("default")) {
            return new ResponseSelection(
                    "default",
                    responseNode(root, responses, "default")
            );
        }
        throw invalid("responses must contain a 2xx response or default");
    }

    private JsonNode responseNode(JsonNode root, JsonNode responses, String status) {
        JsonNode value = responses.get(status);
        if (value == null || !value.isObject()) {
            throw invalid("response " + status + " must be an object");
        }
        JsonNode reference = value.get("$ref");
        if (reference == null) {
            return value;
        }
        if (!reference.isTextual()
                || !reference.asText().startsWith("#/components/responses/")) {
            throw invalid("response references must remain local");
        }
        JsonNode resolved = root.at(reference.asText().substring(1));
        if (!resolved.isObject()) {
            throw invalid("response reference target was not found");
        }
        return referenceDocumentation(resolved, value);
    }

    private MediaSelection selectMedia(
            JsonNode root,
            JsonNode content,
            String context) {
        if (content == null || !content.isObject() || content.isEmpty()) {
            return null;
        }
        List<String> mediaTypes = mediaTypes(content);
        List<String> candidates = mediaTypes.stream()
                .filter(value -> "application/json".equalsIgnoreCase(value))
                .toList();
        if (candidates.isEmpty()) {
            candidates = mediaTypes.stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT)
                            .startsWith("application/")
                            && value.toLowerCase(Locale.ROOT).endsWith("+json"))
                    .toList();
        }
        if (candidates.isEmpty()) {
            if (mediaTypes.size() != 1) {
                throw invalid(context + " media types are ambiguous");
            }
            candidates = List.of(mediaTypes.getFirst());
        }
        String first = candidates.getFirst();
        JsonNode schema = content.path(first).path("schema");
        String fingerprint = schemaFingerprint(root, schema);
        for (String candidate : candidates) {
            JsonNode candidateSchema = content.path(candidate).path("schema");
            if (!fingerprint.equals(schemaFingerprint(root, candidateSchema))) {
                throw invalid(context + " media schemas are ambiguous");
            }
        }
        return new MediaSelection(first, schema);
    }

    private String schemaFingerprint(JsonNode root, JsonNode schema) {
        try {
            return HexFormatHolder.sha256(
                    canonicalTree(
                            schema == null
                                    ? objectMapper.createObjectNode()
                                    : schema,
                            null
                    )
            );
        } catch (JsonProcessingException failure) {
            throw invalid("schema cannot be canonicalized");
        }
    }

    private JsonNode canonicalTree(JsonNode value, String fieldName) {
        if (value == null || value.isValueNode()) {
            return value;
        }
        if (value.isArray()) {
            List<JsonNode> values = new ArrayList<>();
            value.elements().forEachRemaining(item -> values.add(
                    canonicalTree(item, fieldName)
            ));
            if ("required".equals(fieldName)
                    || "enum".equals(fieldName)
                    || "permissions".equals(fieldName)) {
                values.sort(Comparator.comparing(JsonNode::toString));
            }
            ArrayNode array = objectMapper.createArrayNode();
            values.forEach(array::add);
            return array;
        }
        ObjectNode object = objectMapper.createObjectNode();
        List<String> fields = new ArrayList<>();
        value.fieldNames().forEachRemaining(fields::add);
        fields.sort(Comparator.naturalOrder());
        for (String field : fields) {
            object.set(field, canonicalTree(value.get(field), field));
        }
        return object;
    }

    private List<String> mediaTypes(JsonNode content) {
        if (content == null || !content.isObject()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        content.fieldNames().forEachRemaining(result::add);
        return result.stream().map(String::trim).filter(value -> !value.isEmpty())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private int compareResponseStatus(String left, String right) {
        if (left.equals(right)) {
            return 0;
        }
        if ("default".equals(left)) {
            return 1;
        }
        if ("default".equals(right)) {
            return -1;
        }
        try {
            return Integer.compare(
                    Integer.parseInt(left),
                    Integer.parseInt(right)
            );
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }

    private void requireObject(JsonNode value, String label) {
        if (value == null || !value.isObject()) {
            throw invalid(label + " must be an object");
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank()
                ? value.asText().trim()
                : fallback;
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static ObjectMapper deterministicMapper() {
        return new ObjectMapper()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    private int schemaNodes;

    private record ParameterKey(String location, String name) {
    }

    private record MediaSelection(String mediaType, JsonNode schema) {
    }

    private record ResponseSelection(String status, JsonNode response) {
    }

    private static final class HexFormatHolder {

        private HexFormatHolder() {
        }

        private static String sha256(JsonNode value)
                throws JsonProcessingException {
            try {
                byte[] bytes = deterministicMapper().writeValueAsBytes(value);
                return java.util.HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes)
                );
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }
}
