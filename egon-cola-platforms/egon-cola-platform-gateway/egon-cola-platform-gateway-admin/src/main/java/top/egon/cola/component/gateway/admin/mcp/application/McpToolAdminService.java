package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpManagedToolOverrideStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteToolDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class McpToolAdminService {

    private static final String IDEMPOTENCY_SCOPE = "GATEWAY_MCP";

    private static final List<String> RISK_LEVELS = List.of(
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );

    private final McpReleaseContentFactory contentFactory;

    private final McpValidationService validation;

    private final JdbcMcpManagedToolOverrideStore managedOverrides;

    private final JdbcMcpRemoteToolDraftStore remoteTools;

    private final JdbcMcpRemoteProviderStore remote;

    private final McpServerRepository servers;

    private final GatewayDraftRepository drafts;

    private final IdempotencyStore idempotency;

    private final GatewayAuditLogRepository audits;

    private final ObjectMapper objectMapper;

    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    private final Clock clock;

    @Autowired
    public McpToolAdminService(
            McpReleaseContentFactory contentFactory,
            McpValidationService validation,
            JdbcMcpManagedToolOverrideStore managedOverrides,
            JdbcMcpRemoteToolDraftStore remoteTools,
            JdbcMcpRemoteProviderStore remote,
            McpServerRepository servers,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper) {
        this(
                contentFactory,
                validation,
                managedOverrides,
                remoteTools,
                remote,
                servers,
                drafts,
                idempotency,
                audits,
                objectMapper,
                Clock.systemUTC()
        );
    }

    McpToolAdminService(
            McpReleaseContentFactory contentFactory,
            McpValidationService validation,
            JdbcMcpManagedToolOverrideStore managedOverrides,
            JdbcMcpRemoteToolDraftStore remoteTools,
            JdbcMcpRemoteProviderStore remote,
            McpServerRepository servers,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper,
            Clock clock) {
        this.contentFactory = contentFactory;
        this.validation = validation;
        this.managedOverrides = managedOverrides;
        this.remoteTools = remoteTools;
        this.remote = remote;
        this.servers = servers;
        this.drafts = drafts;
        this.idempotency = idempotency;
        this.audits = audits;
        this.objectMapper = objectMapper.copy();
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ManagedToolView> managedTools(
            String gatewayGroupId,
            String serverId) {
        String groupId = required(gatewayGroupId, "gatewayGroupId");
        if (serverId != null) {
            requiredServerInGroup(serverId, groupId);
        }
        return contentFactory.managedTools(groupId).stream()
                .filter(item -> serverId == null
                        || serverId.equals(item.serverId()))
                .map(this::managedView)
                .toList();
    }

    @Transactional
    public McpControlPlaneService.MutationResult putOverride(
            String toolId,
            ManagedToolOverrideMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("PUT_MANAGED_TOOL_OVERRIDE", Map.of(
                "toolId", toolId,
                "command", command
        ));
        McpControlPlaneService.MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpReleaseContentFactory.ManagedToolProjection managed =
                requiredManaged(command.gatewayGroupId(), toolId);
        if (Boolean.TRUE.equals(command.enabled())) {
            throw new IllegalArgumentException(
                    "managed Tool override can only disable a Tool"
            );
        }
        String serverId = optional(command.serverId());
        if (serverId != null) {
            requiredServerInGroup(serverId, command.gatewayGroupId());
        }
        Set<String> additions = clean(command.additionalPermissions());
        if (!additions.containsAll(managed.additionalPermissions())) {
            throw new IllegalArgumentException(
                    "additionalPermissions cannot remove existing permissions"
            );
        }
        String minimumRisk = optional(command.minimumRiskLevel());
        if (minimumRisk != null
                && risk(minimumRisk) < risk(managed.codeRiskLevel())) {
            throw new IllegalArgumentException(
                    "minimumRiskLevel cannot lower annotation riskLevel"
            );
        }
        String currentMinimumRisk = managed.minimumRiskLevel();
        if (currentMinimumRisk != null
                && (minimumRisk == null
                || risk(minimumRisk) < risk(currentMinimumRisk))) {
            throw new IllegalArgumentException(
                    "minimumRiskLevel cannot lower existing minimum risk"
            );
        }
        if (!managed.tool().enabled()
                && !Boolean.FALSE.equals(command.enabled())) {
            throw new IllegalArgumentException(
                    "disabled managed Tool can only be restored by deleting "
                            + "its override"
            );
        }
        GatewayDraftEntity draft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = managedOverrides.save(
                new JdbcMcpManagedToolOverrideStore.ManagedToolOverride(
                        toolId,
                        command.gatewayGroupId(),
                        managed.tool().operationId(),
                        serverId,
                        additions,
                        minimumRisk,
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        validation.requireValid(contentFactory.preview(
                command.gatewayGroupId()
        ));
        return finish(
                draft,
                toolId,
                mutation.revision(),
                "MCP_MANAGED_TOOL_OVERRIDE",
                "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("operationId", managed.tool().operationId()),
                now
        );
    }

    @Transactional
    public McpControlPlaneService.MutationResult deleteOverride(
            String toolId,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_MANAGED_TOOL_OVERRIDE", Map.of(
                "toolId", toolId,
                "control", control
        ));
        McpControlPlaneService.MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpReleaseContentFactory.ManagedToolProjection managed =
                requiredManaged(control.gatewayGroupId(), toolId);
        GatewayDraftEntity draft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = managedOverrides.delete(
                toolId,
                control.gatewayGroupId(),
                managed.tool().operationId(),
                control.expectedRevision()
        );
        validation.requireValid(contentFactory.preview(
                control.gatewayGroupId()
        ));
        return finish(
                draft,
                toolId,
                mutation.revision(),
                "MCP_MANAGED_TOOL_OVERRIDE",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("operationId", managed.tool().operationId()),
                now
        );
    }

    @Transactional(readOnly = true)
    public List<RemoteToolView> remoteTools(
            String gatewayGroupId,
            String serverId) {
        String groupId = required(gatewayGroupId, "gatewayGroupId");
        Map<String, McpServerEntity> serverById = servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        groupId
                ).stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                        McpServerEntity::getId,
                        java.util.function.Function.identity()
                ));
        if (serverId != null && !serverById.containsKey(serverId)) {
            throw notFound("MCP Server", serverId);
        }
        return remoteTools.load(groupId).stream()
                .filter(item -> serverId == null
                        || serverId.equals(item.serverId()))
                .map(item -> remoteView(item, serverById.get(item.serverId())))
                .toList();
    }

    @Transactional
    public McpControlPlaneService.MutationResult putRemoteTool(
            String id,
            RemoteToolMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = id == null
                ? digest("CREATE_REMOTE_TOOL", command)
                : digest("UPDATE_REMOTE_TOOL", Map.of(
                        "id", id,
                        "command", command
                ));
        McpControlPlaneService.MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        String resourceId = id == null ? UuidV7.simpleString() : id;
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        requireRemoteMount(
                command.remoteMountId(),
                command.gatewayGroupId(),
                command.serverId()
        );
        if (id != null) {
            var existing = requiredRemoteTool(
                    command.gatewayGroupId(),
                    id
            );
            if (!existing.serverId().equals(command.serverId())) {
                throw new IllegalArgumentException(
                        "remote MCP Tool cannot move between Servers"
                );
            }
        }
        risk(defaulted(command.riskLevel(), "LOW"));
        GatewayDraftEntity draft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remoteTools.save(
                new JdbcMcpRemoteToolDraftStore.RemoteToolDraft(
                        resourceId,
                        command.gatewayGroupId(),
                        command.serverId(),
                        command.name(),
                        command.remoteMountId(),
                        remoteContent(command),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        validation.requireValid(contentFactory.preview(
                command.gatewayGroupId()
        ));
        return finish(
                draft,
                resourceId,
                mutation.revision(),
                "MCP_REMOTE_TOOL",
                id == null ? "CREATE" : "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("name", command.name()),
                now
        );
    }

    @Transactional
    public McpControlPlaneService.MutationResult deleteRemoteTool(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_TOOL", Map.of(
                "id", id,
                "control", control
        ));
        McpControlPlaneService.MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        var existing = requiredRemoteTool(control.gatewayGroupId(), id);
        GatewayDraftEntity draft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remoteTools.softDelete(
                id,
                control.expectedRevision(),
                actor,
                now
        );
        validation.requireValid(contentFactory.preview(
                control.gatewayGroupId()
        ));
        return finish(
                draft,
                id,
                mutation.revision(),
                "MCP_REMOTE_TOOL",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("name", existing.name()),
                now
        );
    }

    private ManagedToolView managedView(
            McpReleaseContentFactory.ManagedToolProjection value) {
        var tool = value.tool();
        return new ManagedToolView(
                tool.toolId(),
                value.gatewayGroupId(),
                tool.operationId(),
                value.operationKey(),
                tool.name(),
                tool.description(),
                tool.operationProtocol(),
                schema(tool.inputSchema()),
                schema(tool.outputSchema()),
                value.codeServerId(),
                value.codeServerCode(),
                value.serverId(),
                tool.serverCode(),
                value.codePermissions(),
                value.additionalPermissions(),
                tool.requiredPermissions(),
                value.codeRiskLevel(),
                value.minimumRiskLevel(),
                tool.riskLevel(),
                tool.idempotent(),
                tool.enabled(),
                value.overrideRevision()
        );
    }

    private RemoteToolView remoteView(
            JdbcMcpRemoteToolDraftStore.RemoteToolDraft value,
            McpServerEntity server) {
        if (server == null) {
            throw notFound("MCP Server", value.serverId());
        }
        Map<String, Object> content = value.content();
        return new RemoteToolView(
                value.id(),
                value.gatewayGroupId(),
                value.serverId(),
                server.getServerCode(),
                value.name(),
                optionalText(content.get("description")),
                value.remoteMountId(),
                content.get("inputSchema"),
                content.get("outputSchema"),
                stringMap(content.get("annotations")),
                clean(content.get("requiredPermissions")),
                defaulted(optionalText(content.get("riskLevel")), "LOW"),
                flag(content.get("idempotent")),
                value.enabled(),
                value.revision()
        );
    }

    private Map<String, Object> remoteContent(RemoteToolMutation command) {
        Map<String, Object> content = new LinkedHashMap<>();
        put(content, "description", command.description());
        put(content, "inputSchema", command.inputSchema());
        put(content, "outputSchema", command.outputSchema());
        if (command.annotations() != null && !command.annotations().isEmpty()) {
            content.put("annotations", Map.copyOf(command.annotations()));
        }
        Set<String> permissions = clean(command.requiredPermissions());
        if (!permissions.isEmpty()) {
            content.put("requiredPermissions", permissions);
        }
        content.put("riskLevel", defaulted(command.riskLevel(), "LOW"));
        content.put("idempotent", command.idempotent());
        return Map.copyOf(content);
    }

    private void requireRemoteMount(
            String mountId,
            String gatewayGroupId,
            String serverId) {
        var mount = remote.mounts(gatewayGroupId).stream()
                .filter(item -> item.id().equals(mountId))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Remote Mount", mountId));
        if (!mount.serverId().equals(serverId)) {
            throw new IllegalArgumentException(
                    "remote MCP Tool mount belongs to another Server"
            );
        }
    }

    private McpReleaseContentFactory.ManagedToolProjection requiredManaged(
            String gatewayGroupId,
            String toolId) {
        return contentFactory.managedTools(gatewayGroupId).stream()
                .filter(item -> item.tool().toolId().equals(toolId))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Managed Tool", toolId));
    }

    private JdbcMcpRemoteToolDraftStore.RemoteToolDraft requiredRemoteTool(
            String gatewayGroupId,
            String id) {
        return remoteTools.load(gatewayGroupId).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Remote Tool", id));
    }

    private McpServerEntity requiredServerInGroup(
            String id,
            String gatewayGroupId) {
        McpServerEntity server = servers.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("MCP Server", id));
        if (!server.getGatewayGroupId().equals(gatewayGroupId)) {
            throw new IllegalArgumentException(
                    "MCP resource belongs to another Gateway Group"
            );
        }
        return server;
    }

    private GatewayDraftEntity editable(
            String gatewayGroupId,
            long expectedRevision) {
        GatewayDraftEntity draft = drafts.findById(gatewayGroupId)
                .orElseThrow(() -> notFound("Gateway Draft", gatewayGroupId));
        draft.assertEditable(expectedRevision);
        return draft;
    }

    private McpControlPlaneService.MutationResult finish(
            GatewayDraftEntity draft,
            String resourceId,
            long resourceRevision,
            String resourceType,
            String action,
            String changeReason,
            String idempotencyKey,
            String payloadDigest,
            AdminActor actor,
            RequestAuditContext request,
            Map<String, Object> summary,
            Instant now) {
        draft.touch(required(changeReason, "changeReason"), actor.actorId(), now);
        drafts.flush();
        var result = new McpControlPlaneService.MutationResult(
                draft.getRevision(),
                resourceId,
                resourceRevision,
                false
        );
        idempotency.save(new IdempotencyStore.Record(
                IDEMPOTENCY_SCOPE,
                draft.getGatewayGroupId(),
                required(idempotencyKey, "idempotencyKey"),
                payloadDigest,
                resourceId,
                Map.of(
                        "draftRevision", result.draftRevision(),
                        "resourceRevision", result.resourceRevision()
                ),
                now,
                now.plus(Duration.ofDays(7))
        ));
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                resourceType,
                resourceId,
                action,
                null,
                summary,
                null,
                null,
                true,
                null,
                now
        ));
        return result;
    }

    private McpControlPlaneService.MutationResult replay(
            String gatewayGroupId,
            String idempotencyKey,
            String payloadDigest) {
        IdempotencyStore.Record existing = idempotency.find(
                IDEMPOTENCY_SCOPE,
                gatewayGroupId,
                required(idempotencyKey, "idempotencyKey")
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(payloadDigest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        return new McpControlPlaneService.MutationResult(
                number(existing.response(), "draftRevision"),
                existing.resourceId(),
                number(existing.response(), "resourceRevision"),
                true
        );
    }

    private String digest(String action, Object command) {
        return GatewayRuleCanonicalizer.sha256(canonicalizer.canonicalBytes(
                Map.of("action", action, "command", command)
        ));
    }

    private int risk(String value) {
        int level = RISK_LEVELS.indexOf(value);
        if (level < 0) {
            throw new IllegalArgumentException(
                    "unsupported MCP Tool risk level " + value
            );
        }
        return level;
    }

    private Object schema(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("stored MCP schema is invalid", failure);
        }
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(
                key.toString(),
                Objects.toString(item, "")
        ));
        return Map.copyOf(result);
    }

    private Set<String> clean(Object value) {
        if (value == null) {
            return Set.of();
        }
        Iterable<?> values = value instanceof Iterable<?> iterable
                ? iterable
                : List.of(value);
        Set<String> result = new LinkedHashSet<>();
        values.forEach(item -> {
            String text = optionalText(item);
            if (text != null) {
                result.add(text);
            }
        });
        return Set.copyOf(result);
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private boolean flag(Object value) {
        return value instanceof Boolean bool
                ? bool
                : Boolean.parseBoolean(Objects.toString(value, "false"));
    }

    private long number(Map<String, Object> value, String key) {
        Object item = value.get(key);
        return item instanceof Number number
                ? number.longValue()
                : Long.parseLong(item.toString());
    }

    private String required(String value, String field) {
        String result = optional(value);
        if (result == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return result;
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String optionalText(Object value) {
        return value == null ? null : optional(value.toString());
    }

    private String defaulted(String value, String defaultValue) {
        String result = optional(value);
        return result == null ? defaultValue : result;
    }

    private GatewayAdminNotFoundException notFound(
            String resource,
            String id) {
        return new GatewayAdminNotFoundException(
                resource + " " + id + " was not found"
        );
    }

    public record ManagedToolOverrideMutation(
            String gatewayGroupId,
            Boolean enabled,
            String serverId,
            Set<String> additionalPermissions,
            String minimumRiskLevel,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record RemoteToolMutation(
            String gatewayGroupId,
            String serverId,
            String name,
            String description,
            String remoteMountId,
            Object inputSchema,
            Object outputSchema,
            Map<String, String> annotations,
            Set<String> requiredPermissions,
            String riskLevel,
            boolean idempotent,
            boolean enabled,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record MutationControl(
            String gatewayGroupId,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record ManagedToolView(
            String toolId,
            String gatewayGroupId,
            String operationId,
            String operationKey,
            String name,
            String description,
            String operationProtocol,
            Object inputSchema,
            Object outputSchema,
            String codeServerId,
            String codeServerCode,
            String serverId,
            String serverCode,
            Set<String> codePermissions,
            Set<String> additionalPermissions,
            Set<String> effectivePermissions,
            String codeRiskLevel,
            String minimumRiskLevel,
            String effectiveRiskLevel,
            boolean idempotent,
            boolean enabled,
            long overrideRevision
    ) {
    }

    public record RemoteToolView(
            String id,
            String gatewayGroupId,
            String serverId,
            String serverCode,
            String name,
            String description,
            String remoteMountId,
            Object inputSchema,
            Object outputSchema,
            Map<String, String> annotations,
            Set<String> requiredPermissions,
            String riskLevel,
            boolean idempotent,
            boolean enabled,
            long revision
    ) {
    }
}
