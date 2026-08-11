package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpManagedToolOverrideStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteToolDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class McpReleaseContentFactory {

    private static final List<String> RISK_LEVELS = List.of(
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );

    private final McpServerRepository servers;

    private final JdbcMcpCapabilityDraftStore capabilities;

    private final JdbcMcpManagedToolOverrideStore managedOverrides;

    private final JdbcMcpRemoteToolDraftStore remoteTools;

    private final JdbcMcpRemoteProviderStore remote;

    private final JdbcMcpArtifactMetadataStore artifacts;

    private final GatewayDraftRepository drafts;

    private final GatewayCatalogStore catalog;

    private final McpValidationService validation;

    private final ObjectMapper objectMapper;

    public McpReleaseContentFactory(
            McpServerRepository servers,
            JdbcMcpCapabilityDraftStore capabilities,
            JdbcMcpManagedToolOverrideStore managedOverrides,
            JdbcMcpRemoteToolDraftStore remoteTools,
            JdbcMcpRemoteProviderStore remote,
            JdbcMcpArtifactMetadataStore artifacts,
            GatewayDraftRepository drafts,
            GatewayCatalogStore catalog,
            McpValidationService validation,
            ObjectMapper objectMapper) {
        this.servers = servers;
        this.capabilities = capabilities;
        this.managedOverrides = managedOverrides;
        this.remoteTools = remoteTools;
        this.remote = remote;
        this.artifacts = artifacts;
        this.drafts = drafts;
        this.catalog = catalog;
        this.validation = validation;
        this.objectMapper = objectMapper.copy();
    }

    @Transactional(readOnly = true)
    public McpRuleContent compileForRelease(
            String gatewayGroupId,
            long expectedDraftRevision) {
        drafts.findById(gatewayGroupId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway draft " + gatewayGroupId + " was not found"
                ))
                .assertEditable(expectedDraftRevision);
        McpRuleContent content = create(gatewayGroupId);
        validation.requireValid(content);
        return content;
    }

    @Transactional(readOnly = true)
    public McpRuleContent preview(String gatewayGroupId) {
        return create(gatewayGroupId);
    }

    @Transactional(readOnly = true)
    public List<ManagedToolProjection> managedTools(String gatewayGroupId) {
        List<McpServerEntity> serverEntities = servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        gatewayGroupId
                );
        return managedTools(gatewayGroupId, serverEntities);
    }

    private McpRuleContent create(String gatewayGroupId) {
        List<McpServerEntity> serverEntities = servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        gatewayGroupId
                );
        Map<String, McpServerEntity> serverById = serverEntities.stream()
                .collect(Collectors.toUnmodifiableMap(
                        McpServerEntity::getId,
                        Function.identity()
                ));
        var draft = capabilities.load(gatewayGroupId);
        List<JdbcMcpRemoteProviderStore.RemoteProviderDraft> providers =
                remote.providers(gatewayGroupId);
        Map<String, JdbcMcpRemoteProviderStore.RemoteProviderDraft>
                providerById = providers.stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                        JdbcMcpRemoteProviderStore.RemoteProviderDraft::id,
                        java.util.function.Function.identity()
                )
        );

        return new McpRuleContent(
                serverEntities.stream().map(this::server).toList(),
                tools(gatewayGroupId, serverEntities, serverById),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.RESOURCE
                ).stream().map(item -> resource(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind
                                .RESOURCE_TEMPLATE
                ).stream().map(item -> template(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.PROMPT
                ).stream().map(item -> prompt(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.TASK_POLICY
                ).stream().map(item -> taskPolicy(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.APP_BINDING
                ).stream().map(item -> app(item, serverById)).toList(),
                providers.stream().map(this::provider).toList(),
                remote.mounts(gatewayGroupId).stream()
                        .map(item -> mount(
                                item,
                                serverById,
                                providerById
                        ))
                        .toList()
        );
    }

    private List<McpRuntimeTool> tools(
            String gatewayGroupId,
            List<McpServerEntity> serverEntities,
            Map<String, McpServerEntity> serverById) {
        List<McpRuntimeTool> result = new ArrayList<>();
        managedTools(gatewayGroupId, serverEntities).stream()
                .map(ManagedToolProjection::tool)
                .forEach(result::add);
        remoteTools.load(gatewayGroupId).stream()
                .map(item -> remoteTool(item, serverById))
                .forEach(result::add);
        return List.copyOf(result);
    }

    private List<ManagedToolProjection> managedTools(
            String gatewayGroupId,
            List<McpServerEntity> serverEntities) {
        Map<String, McpServerEntity> serverById = serverEntities.stream()
                .collect(Collectors.toUnmodifiableMap(
                        McpServerEntity::getId,
                        Function.identity()
                ));
        Map<String, McpServerEntity> serverByCode = serverEntities.stream()
                .collect(Collectors.toUnmodifiableMap(
                        McpServerEntity::getServerCode,
                        Function.identity()
                ));
        Map<String, JdbcMcpManagedToolOverrideStore.ManagedToolOverride>
                overrideByOperationId = managedOverrides.load(gatewayGroupId)
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        JdbcMcpManagedToolOverrideStore.ManagedToolOverride
                                ::operationId,
                        Function.identity()
                ));
        List<ManagedToolProjection> result = new ArrayList<>();
        for (GatewayCatalogStore.CurrentOperationDefinition current
                : catalog.loadCurrentOperationDefinitions(gatewayGroupId)) {
            if (!"STARTER".equals(current.operation().sourceType())) {
                continue;
            }
            Map<String, Object> exposure = objectMap(
                    current.definition().attributes().get("mcpExposure")
            );
            if (!bool(exposure, "registerMcp", false)) {
                continue;
            }
            result.add(managedTool(
                    gatewayGroupId,
                    current,
                    exposure,
                    serverById,
                    serverByCode,
                    overrideByOperationId
            ));
        }
        return List.copyOf(result);
    }

    private ManagedToolProjection managedTool(
            String gatewayGroupId,
            GatewayCatalogStore.CurrentOperationDefinition current,
            Map<String, Object> exposure,
            Map<String, McpServerEntity> serverById,
            Map<String, McpServerEntity> serverByCode,
            Map<String, JdbcMcpManagedToolOverrideStore.ManagedToolOverride>
                    overrideByOperationId) {
        GatewayCatalogStore.OperationRecord operation = current.operation();
        GatewayCatalogStore.OperationDefinition definition =
                current.definition();
        String codeServerCode = required(exposure, "mcpServerCode");
        McpServerEntity codeServer = serverByCode.get(codeServerCode);
        if (codeServer == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    "operations." + operation.operationKey()
                            + ".mcpExposure.mcpServerCode",
                    "MCP Server " + codeServerCode + " was not found"
            );
        }
        String toolId = managedToolId(codeServerCode, operation.operationKey());
        JdbcMcpManagedToolOverrideStore.ManagedToolOverride override =
                overrideByOperationId.get(operation.id());
        McpServerEntity effectiveServer = override == null
                || override.serverId() == null
                ? codeServer
                : serverById.get(override.serverId());
        if (effectiveServer == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    "managedTools." + toolId + ".serverId",
                    "override MCP Server was not found"
            );
        }
        Set<String> codePermissions = strings(
                exposure.get("requiredPermissions")
        );
        Set<String> additionalPermissions = override == null
                ? Set.of()
                : override.additionalPermissions();
        LinkedHashSet<String> effectivePermissions = new LinkedHashSet<>(
                codePermissions
        );
        effectivePermissions.addAll(additionalPermissions);
        String codeRisk = text(exposure, "riskLevel", "LOW");
        String minimumRisk = override == null
                ? null
                : override.minimumRiskLevel();
        String effectiveRisk = maximumRisk(codeRisk, minimumRisk);
        if (!Set.of("HTTP", "RPC").contains(operation.protocol())) {
            throw new McpValidationException(
                    "GATEWAY_MCP_OPERATION_PROTOCOL_UNSUPPORTED",
                    "operations." + operation.operationKey() + ".protocol",
                    "managed MCP Tool requires HTTP or RPC Operation"
            );
        }
        if ("HTTP".equals(operation.protocol())
                && bool(definition.attributes(), "streaming", false)) {
            throw new McpValidationException(
                    "GATEWAY_MCP_STREAMING_UNSUPPORTED",
                    "operations." + operation.operationKey() + ".streaming",
                    "streaming Operation cannot be projected as an MCP Tool"
            );
        }
        boolean enabled = override == null || override.enabled() == null;
        McpRuntimeTool tool = new McpRuntimeTool(
                toolId,
                effectiveServer.getServerCode(),
                required(exposure, "mcpName"),
                description(definition),
                "LOCAL_OPERATION",
                operation.id(),
                operation.protocol(),
                null,
                inputSchema(operation.protocol(), definition.requestSchema()),
                schema(definition.responseSchema()),
                Map.of(),
                Set.copyOf(effectivePermissions),
                effectiveRisk,
                bool(exposure, "idempotent", false),
                enabled
        );
        return new ManagedToolProjection(
                gatewayGroupId,
                operation.operationKey(),
                codeServer.getId(),
                codeServer.getServerCode(),
                effectiveServer.getId(),
                codePermissions,
                additionalPermissions,
                codeRisk,
                minimumRisk,
                override == null ? 0 : override.revision(),
                tool
        );
    }

    private McpRuntimeServer server(McpServerEntity server) {
        return new McpRuntimeServer(
                server.getId(),
                server.getServerCode(),
                server.getDisplayName(),
                server.getDescription(),
                server.getInstructions(),
                server.getDialects().stream()
                        .map(McpProtocolDialect::valueOf)
                        .collect(java.util.stream.Collectors.toSet()),
                server.getResourceUri(),
                server.getListCacheTtlSeconds(),
                server.isEnabled()
        );
    }

    private McpRuntimeTool remoteTool(
            JdbcMcpRemoteToolDraftStore.RemoteToolDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeTool(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                optional(value, "description"),
                "REMOTE_MCP",
                null,
                null,
                draft.remoteMountId(),
                schema(value.get("inputSchema")),
                schema(value.get("outputSchema")),
                stringMap(value.get("annotations")),
                strings(value.get("requiredPermissions")),
                text(value, "riskLevel", "LOW"),
                bool(value, "idempotent", false),
                draft.enabled()
        );
    }

    private McpRuntimeResource resource(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeResource(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                required(value, "uri"),
                optional(value, "description"),
                text(value, "mimeType", "application/json"),
                required(value, "driverType"),
                optional(value, "operationId"),
                optional(value, "remoteMountId"),
                stringMap(value.get("configuration")),
                strings(value.get("requiredPermissions")),
                number(value, "maxBytes", 67_108_864L),
                draft.enabled()
        );
    }

    private McpRuntimeResourceTemplate template(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeResourceTemplate(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                required(value, "uriTemplate"),
                optional(value, "description"),
                text(value, "mimeType", "application/json"),
                required(value, "driverType"),
                optional(value, "operationId"),
                optional(value, "remoteMountId"),
                stringMap(value.get("configuration")),
                strings(value.get("requiredPermissions")),
                number(value, "maxBytes", 67_108_864L),
                draft.enabled()
        );
    }

    private McpRuntimePrompt prompt(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimePrompt(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                optional(value, "description"),
                required(value, "sourceType"),
                optional(value, "template"),
                optional(value, "operationId"),
                optional(value, "remoteMountId"),
                List.copyOf(strings(value.get("arguments"))),
                strings(value.get("requiredPermissions")),
                draft.enabled()
        );
    }

    private McpRuntimeTaskPolicy taskPolicy(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeTaskPolicy(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                bool(value, "durable", true),
                bool(value, "inputAllowed", false),
                number(value, "executionTimeoutSeconds", 60),
                number(value, "resultTtlSeconds", 86_400),
                Math.toIntExact(number(value, "maxAttempts", 3)),
                draft.enabled()
        );
    }

    private McpRuntimeApp app(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        String artifactId = required(value, "appArtifactId");
        var artifact = artifacts.find(artifactId)
                .orElseThrow(() -> new McpValidationException(
                        "GATEWAY_MCP_ARTIFACT_NOT_FOUND",
                        "apps." + draft.name() + ".appArtifactId",
                        "MCP App artifact was not found"
                ));
        return new McpRuntimeApp(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                artifact.appCode(),
                artifact.version(),
                artifact.resourceUri(),
                artifact.id(),
                artifact.artifactReference(),
                artifact.sha256(),
                artifact.sizeBytes(),
                artifact.mimeType(),
                artifact.contentSecurityPolicy(),
                artifact.permissions(),
                artifact.allowedOrigins(),
                strings(value.get("allowedTools")),
                draft.enabled()
        );
    }

    private McpRuntimeRemoteProvider provider(
            JdbcMcpRemoteProviderStore.RemoteProviderDraft draft) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeRemoteProvider(
                draft.id(),
                draft.providerCode(),
                required(value, "displayName"),
                McpProtocolDialect.valueOf(required(value, "dialect")),
                required(value, "transportType"),
                required(value, "endpointReference"),
                optional(value, "authProfileReference"),
                optional(value, "tlsProfileReference"),
                required(value, "capabilityFingerprint"),
                draft.enabled()
        );
    }

    private McpRuntimeRemoteMount mount(
            JdbcMcpRemoteProviderStore.RemoteMountDraft draft,
            Map<String, McpServerEntity> serverById,
            Map<String, JdbcMcpRemoteProviderStore.RemoteProviderDraft>
                    providerById) {
        var provider = providerById.get(draft.providerId());
        if (provider == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_REMOTE_PROVIDER_NOT_FOUND",
                    "remoteMounts." + draft.id() + ".providerId",
                    "remote MCP Provider was not found"
            );
        }
        Map<String, Object> value = draft.content();
        return new McpRuntimeRemoteMount(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                provider.providerCode(),
                draft.namespace(),
                strings(value.getOrDefault(
                        "primitiveTypes",
                        List.of(
                                "TOOL",
                                "RESOURCE",
                                "RESOURCE_TEMPLATE",
                                "PROMPT",
                                "APP"
                        )
                )),
                stringMap(value.get("renameRules")),
                text(value, "conflictPolicy", "REJECT"),
                strings(value.get("requiredPermissions")),
                draft.capabilityFingerprint(),
                draft.enabled()
        );
    }

    private String serverCode(
            String serverId,
            Map<String, McpServerEntity> serverById) {
        McpServerEntity server = serverById.get(serverId);
        if (server == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    "serverId",
                    "MCP Server " + serverId + " was not found"
            );
        }
        return server.getServerCode();
    }

    private String description(
            GatewayCatalogStore.OperationDefinition definition) {
        String description = optional(definition.attributes(), "description");
        return description == null ? definition.summary() : description;
    }

    private String inputSchema(
            String protocol,
            Map<String, Object> requestSchema) {
        if ("HTTP".equals(protocol)) {
            return schema(httpInputSchema(requestSchema));
        }
        return schema(requestSchema);
    }

    private Map<String, Object> httpInputSchema(
            Map<String, Object> requestSchema) {
        if (requestSchema == null) {
            return null;
        }
        Map<String, Object> projected = new LinkedHashMap<>();
        requestSchema.forEach((key, value) -> {
            if (!"properties".equals(key) && !"required".equals(key)) {
                projected.put(key, value);
            }
        });
        Map<String, Object> properties = objectMap(
                requestSchema.get("properties")
        );
        Map<String, Object> exposedProperties = new LinkedHashMap<>();
        for (String location : List.of("path", "query", "body")) {
            if (properties.containsKey(location)) {
                exposedProperties.put(location, properties.get(location));
            }
        }
        projected.put("properties", exposedProperties);
        Object requiredValue = requestSchema.get("required");
        if (requiredValue instanceof Collection<?> required) {
            List<String> exposedRequired = required.stream()
                    .map(String::valueOf)
                    .filter(List.of("path", "query", "body")::contains)
                    .toList();
            if (!exposedRequired.isEmpty()) {
                projected.put("required", exposedRequired);
            }
        }
        return Map.copyOf(projected);
    }

    static String managedToolId(String serverCode, String operationKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(serverCode.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(operationKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String maximumRisk(String codeRisk, String minimumRisk) {
        int code = risk(codeRisk);
        if (minimumRisk == null) {
            return codeRisk;
        }
        int minimum = risk(minimumRisk);
        return code >= minimum ? codeRisk : minimumRisk;
    }

    private int risk(String value) {
        int level = RISK_LEVELS.indexOf(value);
        if (level < 0) {
            throw new McpValidationException(
                    "GATEWAY_MCP_RISK_INVALID",
                    "riskLevel",
                    "unsupported MCP Tool risk level " + value
            );
        }
        return level;
    }

    private String required(Map<String, Object> value, String key) {
        String result = optional(value, key);
        if (result == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return result;
    }

    private String optional(Map<String, Object> value, String key) {
        Object result = value.get(key);
        return result == null || result.toString().isBlank()
                ? null
                : result.toString().trim();
    }

    private String text(
            Map<String, Object> value,
            String key,
            String defaultValue) {
        String result = optional(value, key);
        return result == null ? defaultValue : result;
    }

    private boolean bool(
            Map<String, Object> value,
            String key,
            boolean defaultValue) {
        Object result = value.get(key);
        return result == null
                ? defaultValue
                : result instanceof Boolean bool
                ? bool
                : Boolean.parseBoolean(result.toString());
    }

    private long number(
            Map<String, Object> value,
            String key,
            long defaultValue) {
        Object result = value.get(key);
        if (result == null) {
            return defaultValue;
        }
        return result instanceof Number number
                ? number.longValue()
                : Long.parseLong(result.toString());
    }

    private Set<String> strings(Object value) {
        if (value == null) {
            return Set.of();
        }
        Collection<?> source = value instanceof Collection<?> collection
                ? collection
                : List.of(value);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        source.forEach(item -> result.add(item.toString().trim()));
        return Set.copyOf(result);
    }

    private Map<String, String> stringMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException("MCP mapping must be an object");
        }
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(
                key.toString(),
                Objects.toString(item, "")
        ));
        return Map.copyOf(result);
    }

    private Map<String, Object> objectMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException("MCP value must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(key.toString(), item));
        return Map.copyOf(result);
    }

    private String schema(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "MCP JSON Schema cannot be serialized",
                    failure
            );
        }
    }

    public record ManagedToolProjection(
            String gatewayGroupId,
            String operationKey,
            String codeServerId,
            String codeServerCode,
            String serverId,
            Set<String> codePermissions,
            Set<String> additionalPermissions,
            String codeRiskLevel,
            String minimumRiskLevel,
            long overrideRevision,
            McpRuntimeTool tool
    ) {
    }

}
