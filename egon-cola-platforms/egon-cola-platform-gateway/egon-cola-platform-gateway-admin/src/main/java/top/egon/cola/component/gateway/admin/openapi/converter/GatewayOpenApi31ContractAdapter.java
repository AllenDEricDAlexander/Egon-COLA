package top.egon.cola.component.gateway.admin.openapi.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionSourceTypeEnum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Adapts one validated OpenAPI 3.1 Group into the normalized Report v2 graph.
 *
 * <p>中文：这里承载 OpenAPI 图遍历、canonicalization、Group/domain 归一化和
 * OperationKey 构造；结构性 DTO 到 Report 的复制交给 MapStruct converter。</p>
 */
@Slf4j
@Component("gatewayOpenApi31ContractAdapter")
public class GatewayOpenApi31ContractAdapter {

    private static final String REPORT_VERSION = "v2";

    private static final List<String> HTTP_METHODS = List.of(
            "get", "put", "post", "delete", "options", "head", "patch",
            "trace"
    );

    private static final Set<String> HTTP_METHOD_SET = Set.copyOf(HTTP_METHODS);

    private final ObjectMapper objectMapper;

    private final GatewayOpenApiInvocationSchemaAdapter schemaAdapter;

    /** Creates an adapter with deterministic Jackson and schema traversal. */
    public GatewayOpenApi31ContractAdapter() {
        this(deterministicMapper());
    }

    /** Creates an adapter using the application's Jackson mapper. */
    public GatewayOpenApi31ContractAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.schemaAdapter = new GatewayOpenApiInvocationSchemaAdapter(
                objectMapper
        );
    }

    /**
     * Adapts a Group using a deterministic fallback application scope.
     * Production orchestration should use the overload carrying the persisted
     * Gateway application scope.
     */
    public GatewayOpenApiDefinitionDTO adapt(
            GatewayOpenApiDocumentDTO document) {
        Objects.requireNonNull(document, "document");
        JsonNode root = requireRoot(document);
        String title = text(root.path("info"), "title", "OpenAPI Group");
        GatewayOpenApiDefinitionDTO.Application application =
                new GatewayOpenApiDefinitionDTO.Application(
                        document.candidate().bizCode(),
                        document.candidate().applicationCode(),
                        title,
                        "unknown",
                        "unknown"
                );
        return adapt(document, application);
    }

    /** Adapts a Group with the authoritative persisted application scope. */
    public GatewayOpenApiDefinitionDTO adapt(
            GatewayOpenApiDocumentDTO document,
            GatewayOpenApiDefinitionDTO.Application application) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(application, "application");
        JsonNode root = requireRoot(document);
        CanonicalDocument canonical = canonicalize(document);
        String openapiVersion = requiredText(root, "openapi");
        if (!openapiVersion.matches("3\\.1\\.\\d+")) {
            throw invalid("OpenAPI document must use version 3.1.x");
        }
        JsonNode service = root.get("x-egon-service");
        validateServiceIdentity(document, service);
        JsonNode info = root.get("info");
        String title = text(info, "title", "OpenAPI Group");
        String description = text(info, "description", null);
        String groupCode = document.candidate().openapiGroup();

        List<GatewayOpenApiDefinitionDTO.Operation> operations =
                operations(document, root, groupCode, application, canonical);
        if (operations.isEmpty()) {
            throw invalid("OpenAPI Group must contain an HTTP operation");
        }
        DomainIdentity identity = domainIdentity(
                root,
                groupCode,
                document.candidate().bizCode(),
                title
        );
        GatewayOpenApiDefinitionDTO.InterfaceGroup interfaceGroup =
                new GatewayOpenApiDefinitionDTO.InterfaceGroup(
                        groupCode,
                        title,
                        description,
                        GatewayDefinitionSourceTypeEnum.OPENAPI31,
                        null,
                        "HTTP",
                        Map.of(
                                "openapiGroup", groupCode,
                                "openapiVersion", openapiVersion,
                                "canonicalSha256", canonical.sha256()
                        ),
                        operations
                );
        GatewayOpenApiDefinitionDTO.EntityDomain entity =
                new GatewayOpenApiDefinitionDTO.EntityDomain(
                        identity.entityCode(),
                        identity.entityName(),
                        identity.entityDescription(),
                        List.of(interfaceGroup)
                );
        GatewayOpenApiDefinitionDTO.BusinessDomain business =
                new GatewayOpenApiDefinitionDTO.BusinessDomain(
                        identity.businessCode(),
                        identity.businessName(),
                        identity.businessDescription(),
                        List.of(entity)
                );
        GatewayOpenApiDefinitionDTO.Build build =
                new GatewayOpenApiDefinitionDTO.Build(
                        document.candidate().artifactVersion(),
                        document.candidate().buildId(),
                        Map.of(
                                "openapiCanonicalSha256", canonical.sha256(),
                                "openapiGroup", groupCode,
                                "openapiVersion", openapiVersion
                        )
                );
        List<GatewayOpenApiDefinitionDTO.BusinessDomain> domains =
                List.of(business);
        String fingerprint = definitionFingerprint(
                application,
                build,
                domains
        );
        String definitionSetId = definitionSetId(
                application,
                build,
                fingerprint
        );
        Instant reportedAt = document.fetchedAt();
        return new GatewayOpenApiDefinitionDTO(
                canonical.sha256(),
                REPORT_VERSION,
                "openapi-" + canonical.sha256(),
                reportedAt,
                application,
                build,
                true,
                definitionSetId,
                fingerprint,
                domains
        );
    }

    /** Adapts with explicit environment and authorization namespace values. */
    public GatewayOpenApiDefinitionDTO adapt(
            GatewayOpenApiDocumentDTO document,
            String env,
            String namespace) {
        Objects.requireNonNull(document, "document");
        JsonNode root = requireRoot(document);
        return adapt(
                document,
                new GatewayOpenApiDefinitionDTO.Application(
                        document.candidate().bizCode(),
                        document.candidate().applicationCode(),
                        text(root.path("info"), "title", "OpenAPI Group"),
                        env,
                        namespace
                )
        );
    }

    /** Returns the canonical JSON bytes and lowercase SHA-256 for a document. */
    public CanonicalDocument canonicalize(
            GatewayOpenApiDocumentDTO document) {
        JsonNode root = requireRoot(document);
        JsonNode normalized = canonicalNode(root, CanonicalContext.ROOT, null);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(normalized);
            return new CanonicalDocument(bytes, sha256(bytes), normalized);
        } catch (JsonProcessingException failure) {
            throw invalid("OpenAPI document cannot be canonicalized", failure);
        }
    }

    /** Convenience method returning only the canonical SHA-256. */
    public String canonicalSha256(GatewayOpenApiDocumentDTO document) {
        return canonicalize(document).sha256();
    }

    /** Counts HTTP operations, excluding path-item metadata and shared parameters. */
    public static int operationCount(JsonNode document) {
        int count = 0;
        for (JsonNode pathItem : document.path("paths")) {
            count += (int) HTTP_METHODS.stream()
                    .filter(method -> pathItem.path(method).isObject()).count();
        }
        return count;
    }

    private List<GatewayOpenApiDefinitionDTO.Operation> operations(
            GatewayOpenApiDocumentDTO document,
            JsonNode root,
            String groupCode,
            GatewayOpenApiDefinitionDTO.Application application,
            CanonicalDocument canonical) {
        JsonNode paths = root.get("paths");
        if (paths == null || !paths.isObject()) {
            throw invalid("OpenAPI paths must be an object");
        }
        List<String> pathNames = new ArrayList<>();
        paths.fieldNames().forEachRemaining(pathNames::add);
        pathNames.sort(Comparator.naturalOrder());
        List<GatewayOpenApiDefinitionDTO.Operation> result = new ArrayList<>();
        Set<String> operationIds = new java.util.HashSet<>();
        for (String path : pathNames) {
            JsonNode pathItem = paths.get(path);
            if (!path.startsWith("/") || path.contains("..")
                    || !pathItem.isObject()) {
                throw invalid("OpenAPI path keys must be absolute templates");
            }
            for (String method : HTTP_METHODS) {
                JsonNode operation = pathItem.get(method);
                if (operation == null) {
                    continue;
                }
                if (!operation.isObject()) {
                    throw invalid("OpenAPI operation must be an object");
                }
                String operationId = requiredText(operation, "operationId");
                if (!operationIds.add(operationId)) {
                    throw invalid("operationId must be unique within one Group");
                }
                result.add(operation(
                        document,
                        root,
                        pathItem,
                        path,
                        method,
                        operation,
                        groupCode,
                        application,
                        canonical
                ));
            }
        }
        return result;
    }

    private GatewayOpenApiDefinitionDTO.Operation operation(
            GatewayOpenApiDocumentDTO document,
            JsonNode root,
            JsonNode pathItem,
            String path,
            String method,
            JsonNode operation,
            String groupCode,
            GatewayOpenApiDefinitionDTO.Application application,
            CanonicalDocument canonical) {
        String operationId = requiredText(operation, "operationId");
        validateOperationExtension(operation);
        Map<String, Object> requestSchema = schemaAdapter.requestSchema(
                root,
                pathItem,
                operation
        );
        Map<String, Object> responseSchema = schemaAdapter.responseSchema(
                root,
                operation
        );
        List<Map<String, Object>> errorSchema = schemaAdapter.errorSchemas(
                root,
                operation
        );
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("httpMethod", method.toUpperCase(Locale.ROOT));
        attributes.put("path", path);
        attributes.put("openapiOperationId", operationId);
        attributes.put("openapiGroup", groupCode);
        attributes.put("openapiCanonicalSha256", canonical.sha256());
        attributes.put(
                "consumes",
                schemaAdapter.requestMediaTypes(root, operation)
        );
        attributes.put(
                "produces",
                schemaAdapter.responseMediaTypes(root, operation)
        );
        attributes.put("responseMode", "TRANSPARENT");
        boolean streaming = schemaAdapter.streaming(root, operation);
        attributes.put("streaming", streaming);
        attributes.put("idempotent", idempotent(operation, method));
        mcpExposure(operation, method).ifPresent(exposure -> attributes.put(
                "mcpExposure",
                exposure
        ));

        JsonNode policy = operation.path("x-egon").path("policy");
        String owner = text(policy, "owner", null);
        boolean external = "EXTERNAL".equalsIgnoreCase(
                text(policy, "exposure", "INTERNAL")
        );
        return new GatewayOpenApiDefinitionDTO.Operation(
                GatewayOperationKey.http(
                        application.applicationCode(),
                        method,
                        path
                ).value(),
                "HTTP",
                method.toUpperCase(Locale.ROOT) + " " + path,
                operationId,
                text(operation, "summary", null),
                text(operation, "description", null),
                owner,
                strings(operation.get("tags")),
                external,
                "SUPPORTED",
                new GatewayOpenApiDefinitionDTO.ProviderService(
                        document.candidate().bizCode(),
                        document.candidate().applicationCode(),
                        application.env(),
                        application.namespace(),
                        "HTTP",
                        document.candidate().providerServiceName(),
                        document.candidate().providerGroup(),
                        document.candidate().providerVersion(),
                        "HTTP"
                ),
                requestSchema,
                responseSchema,
                errorSchema,
                null,
                attributes,
                operation.path("deprecated").asBoolean(false)
        );
    }

    private DomainIdentity domainIdentity(
            JsonNode root,
            String groupCode,
            String defaultBusinessCode,
            String title) {
        JsonNode paths = root.path("paths");
        DomainIdentity identity = null;
        for (JsonNode pathItem : iterable(paths.elements())) {
            if (!pathItem.isObject()) {
                continue;
            }
            for (String method : HTTP_METHODS) {
                JsonNode operation = pathItem.get(method);
                if (operation == null || !operation.isObject()) {
                    continue;
                }
                JsonNode catalog = operation.path("x-egon").path("catalog");
                DomainIdentity current = new DomainIdentity(
                        text(catalog, "businessDomainCode", defaultBusinessCode),
                        text(catalog, "businessDomainName", defaultBusinessCode),
                        null,
                        text(catalog, "entityDomainCode", groupCode),
                        text(catalog, "entityDomainName", groupCode),
                        null,
                        text(catalog, "interfaceGroupCode", groupCode)
                );
                if (!groupCode.equals(current.interfaceGroupCode())) {
                    throw invalid(
                            "x-egon.catalog.interfaceGroupCode must match the OpenAPI Group"
                    );
                }
                if (identity == null) {
                    identity = current;
                } else if (!identity.sameDomain(current)) {
                    throw invalid(
                            "all operations in one OpenAPI Group must share catalog domains"
                    );
                }
            }
        }
        return identity == null
                ? new DomainIdentity(
                        defaultBusinessCode,
                        title,
                        null,
                        groupCode,
                        groupCode,
                        null,
                        groupCode
                )
                : identity;
    }

    private void validateServiceIdentity(
            GatewayOpenApiDocumentDTO document,
            JsonNode service) {
        if (service == null || !service.isObject()
                || !service.path("version").isInt()
                || service.path("version").intValue() != 1) {
            throw invalid("x-egon-service version 1 is required");
        }
        assertMatch(
                service,
                "bizCode",
                document.candidate().bizCode(),
                "x-egon-service.bizCode"
        );
        assertMatch(
                service,
                "applicationCode",
                document.candidate().applicationCode(),
                "x-egon-service.applicationCode"
        );
        assertMatch(
                service,
                "artifactVersion",
                document.candidate().artifactVersion(),
                "x-egon-service.artifactVersion"
        );
        assertMatch(
                service,
                "buildId",
                document.candidate().buildId(),
                "x-egon-service.buildId"
        );
        assertMatch(
                service,
                "openapiGroup",
                document.candidate().openapiGroup(),
                "x-egon-service.openapiGroup"
        );
    }

    private void validateOperationExtension(JsonNode operation) {
        JsonNode extension = operation.get("x-egon");
        if (extension != null
                && (!extension.isObject()
                || !extension.path("version").isInt()
                || extension.path("version").intValue() != 1)) {
            throw invalid("x-egon operation extension must use version 1");
        }
    }

    private void assertMatch(
            JsonNode node,
            String field,
            String expected,
            String label) {
        if (!expected.equals(text(node, field, null))) {
            throw invalid(label + " does not match the trusted candidate");
        }
    }

    private java.util.Optional<Map<String, Object>> mcpExposure(
            JsonNode operation,
            String method) {
        JsonNode extension = operation.path("x-egon").path("mcp");
        if (extension.isMissingNode() || extension.isNull()) {
            return java.util.Optional.empty();
        }
        if (!extension.isObject()) {
            throw invalid("x-egon.mcp must be an object");
        }
        JsonNode enabled = extension.get("enabled");
        if (enabled != null && !enabled.isBoolean()) {
            throw invalid("x-egon.mcp enabled must be boolean");
        }
        if (enabled != null && enabled.isBoolean() && !enabled.asBoolean()) {
            return java.util.Optional.empty();
        }
        String serverCode = text(extension, "serverCode", null);
        if (serverCode == null) {
            throw invalid("enabled x-egon.mcp requires serverCode");
        }
        String riskLevel = text(extension, "riskLevel", null);
        if (riskLevel == null
                || !Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL")
                .contains(riskLevel)) {
            throw invalid("enabled x-egon.mcp requires a valid riskLevel");
        }
        List<String> permissions = strings(extension.get("permissions"));
        if (!permissions.equals(stringsInDocumentOrder(
                extension.get("permissions")
        ))) {
            throw invalid("x-egon.mcp permissions must be sorted and unique");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registerMcp", true);
        result.put("mcpServerCode", serverCode);
        result.put(
                "mcpName",
                text(extension, "name", operation.path("operationId").asText())
        );
        result.put("requiredPermissions", permissions);
        result.put("riskLevel", riskLevel);
        result.put("idempotent", idempotent(operation, method));
        return java.util.Optional.of(Map.copyOf(result));
    }

    private boolean idempotent(JsonNode operation, String method) {
        JsonNode policy = operation.path("x-egon").path("policy");
        JsonNode explicit = policy.get("idempotent");
        if (explicit != null && explicit.isBoolean()) {
            return explicit.asBoolean();
        }
        return Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE")
                .contains(method.toUpperCase(Locale.ROOT));
    }

    private List<String> strings(JsonNode values) {
        if (values == null || values.isMissingNode() || values.isNull()) {
            return List.of();
        }
        if (!values.isArray()) {
            throw invalid("OpenAPI string collection must be an array");
        }
        List<String> result = new ArrayList<>();
        values.elements().forEachRemaining(value -> {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw invalid("OpenAPI string collection contains an invalid value");
            }
            result.add(value.asText().trim());
        });
        return result.stream().distinct().sorted().toList();
    }

    private List<String> stringsInDocumentOrder(JsonNode values) {
        if (values == null || values.isMissingNode() || values.isNull()) {
            return List.of();
        }
        if (!values.isArray()) {
            throw invalid("OpenAPI string collection must be an array");
        }
        List<String> result = new ArrayList<>();
        values.elements().forEachRemaining(value -> {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw invalid("OpenAPI string collection contains an invalid value");
            }
            result.add(value.asText().trim());
        });
        return result;
    }

    private JsonNode requireRoot(GatewayOpenApiDocumentDTO document) {
        Objects.requireNonNull(document, "document");
        JsonNode root = document.documentJson();
        if (root == null || !root.isObject()) {
            throw invalid("OpenAPI document must be a JSON object");
        }
        return root;
    }

    private String requiredText(JsonNode node, String field) {
        String value = text(node, field, null);
        if (value == null) {
            throw invalid(field + " is required");
        }
        return value;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank()
                ? value.asText().trim()
                : fallback;
    }

    private JsonNode canonicalNode(
            JsonNode value,
            CanonicalContext context,
            String fieldName) {
        if (value == null || value.isValueNode()) {
            return value;
        }
        if (value.isArray()) {
            List<JsonNode> children = new ArrayList<>();
            value.elements().forEachRemaining(child -> children.add(
                    canonicalNode(child, CanonicalContext.NORMAL, fieldName)
            ));
            if ("required".equals(fieldName) || "enum".equals(fieldName)
                    || "permissions".equals(fieldName)) {
                children.sort(Comparator.comparing(JsonNode::toString));
            } else if ("parameters".equals(fieldName)
                    && (context == CanonicalContext.PATH_ITEM
                    || context == CanonicalContext.OPERATION)) {
                children.sort(Comparator.comparing(this::parameterSortKey));
            } else if ("security".equals(fieldName)) {
                children.sort(Comparator.comparing(JsonNode::toString));
            } else if ("tags".equals(fieldName)
                    && (context == CanonicalContext.ROOT
                    || context == CanonicalContext.OPERATION)) {
                children.sort(Comparator.comparing(this::tagSortKey));
            }
            ArrayNode array = objectMapper.createArrayNode();
            children.forEach(array::add);
            return array;
        }
        ObjectNode object = objectMapper.createObjectNode();
        List<String> fields = new ArrayList<>();
        value.fieldNames().forEachRemaining(fields::add);
        fields.sort(Comparator.naturalOrder());
        for (String field : fields) {
            if ("servers".equals(field)
                    && (context == CanonicalContext.ROOT
                    || context == CanonicalContext.PATH_ITEM
                    || context == CanonicalContext.OPERATION)) {
                continue;
            }
            JsonNode child = value.get(field);
            CanonicalContext childContext = CanonicalContext.NORMAL;
            if (context == CanonicalContext.ROOT && "paths".equals(field)) {
                childContext = CanonicalContext.PATHS;
            } else if (context == CanonicalContext.PATH_ITEM
                    && HTTP_METHOD_SET.contains(field)) {
                childContext = CanonicalContext.OPERATION;
            }
            if (context == CanonicalContext.PATHS && child != null
                    && child.isObject()) {
                childContext = CanonicalContext.PATH_ITEM;
            }
            CanonicalContext valueContext = childContext;
            if (child != null && child.isArray()
                    && childContext == CanonicalContext.NORMAL) {
                valueContext = context;
            }
            object.set(field, canonicalNode(child, valueContext, field));
        }
        return object;
    }

    private String parameterSortKey(JsonNode node) {
        return text(node, "in", "") + "\u0000"
                + text(node, "name", "") + "\u0000"
                + text(node, "$ref", "");
    }

    private String tagSortKey(JsonNode node) {
        return node.isTextual() ? node.asText() : text(node, "name", node.toString());
    }

    private String definitionFingerprint(
            GatewayOpenApiDefinitionDTO.Application application,
            GatewayOpenApiDefinitionDTO.Build build,
            List<GatewayOpenApiDefinitionDTO.BusinessDomain> domains) {
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("application", application);
        material.put("build", build);
        material.put("businessDomains", domains);
        material.put("complete", true);
        material.put("definitionSchemaVersion", REPORT_VERSION);
        return sha256(writeReportIdentity(material));
    }

    private String definitionSetId(
            GatewayOpenApiDefinitionDTO.Application application,
            GatewayOpenApiDefinitionDTO.Build build,
            String fingerprint) {
        return sha256(String.join(
                "\n",
                application.bizCode(),
                application.applicationCode(),
                application.env(),
                application.namespace(),
                build.artifactVersion(),
                build.buildId(),
                fingerprint
        ).getBytes(StandardCharsets.UTF_8));
    }

    private byte[] write(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw invalid("OpenAPI definition identity cannot be serialized", failure);
        }
    }

    private byte[] writeReportIdentity(Object value) {
        try {
            return identityMapper().writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw invalid(
                    "OpenAPI definition identity cannot be serialized",
                    failure
            );
        }
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static ObjectMapper deterministicMapper() {
        return new ObjectMapper()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    private static ObjectMapper identityMapper() {
        return new ObjectMapper()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private IllegalArgumentException invalid(String message, Throwable cause) {
        return new IllegalArgumentException(message, cause);
    }

    private static <T> Iterable<T> iterable(Iterator<T> iterator) {
        return () -> iterator;
    }

    /** Canonical representation handed to snapshot persistence. */
    public record CanonicalDocument(
            byte[] bytes,
            String sha256,
            JsonNode document
    ) {

        public CanonicalDocument {
            bytes = bytes.clone();
            document = document.deepCopy();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        @Override
        public JsonNode document() {
            return document.deepCopy();
        }
    }

    private enum CanonicalContext {
        ROOT,
        PATHS,
        PATH_ITEM,
        OPERATION,
        NORMAL
    }

    private record DomainIdentity(
            String businessCode,
            String businessName,
            String businessDescription,
            String entityCode,
            String entityName,
            String entityDescription,
            String interfaceGroupCode
    ) {

        private boolean sameDomain(DomainIdentity other) {
            return businessCode.equals(other.businessCode)
                    && entityCode.equals(other.entityCode)
                    && interfaceGroupCode.equals(other.interfaceGroupCode);
        }
    }
}
