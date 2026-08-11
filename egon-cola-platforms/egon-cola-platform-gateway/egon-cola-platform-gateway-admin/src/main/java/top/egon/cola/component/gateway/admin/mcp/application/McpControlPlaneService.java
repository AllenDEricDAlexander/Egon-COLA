package top.egon.cola.component.gateway.admin.mcp.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpTaskStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class McpControlPlaneService {

    private static final String IDEMPOTENCY_SCOPE = "GATEWAY_MCP";

    private static final String TASK_IDEMPOTENCY_SCOPE = "GATEWAY_MCP_TASK";

    private final McpServerRepository servers;

    private final JdbcMcpCapabilityDraftStore capabilities;

    private final JdbcMcpRemoteProviderStore remote;

    private final JdbcMcpArtifactMetadataStore artifacts;

    private final McpAppArtifactStore.Writer artifactWriter;

    private final McpAppArtifactStore.Reader artifactReader;

    private final McpAppSecurityValidator appSecurity;

    private final JdbcMcpTaskStore tasks;

    private final GatewayDraftRepository drafts;

    private final IdempotencyStore idempotency;

    private final GatewayAuditLogRepository audits;

    private final McpReleaseContentFactory contentFactory;

    private final McpValidationService validation;

    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    private final Clock clock;

    @Autowired
    public McpControlPlaneService(
            McpServerRepository servers,
            JdbcMcpCapabilityDraftStore capabilities,
            JdbcMcpRemoteProviderStore remote,
            JdbcMcpArtifactMetadataStore artifacts,
            McpAppArtifactStore.Writer artifactWriter,
            McpAppArtifactStore.Reader artifactReader,
            McpAppSecurityValidator appSecurity,
            JdbcMcpTaskStore tasks,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            McpReleaseContentFactory contentFactory,
            McpValidationService validation) {
        this(
                servers,
                capabilities,
                remote,
                artifacts,
                artifactWriter,
                artifactReader,
                appSecurity,
                tasks,
                drafts,
                idempotency,
                audits,
                contentFactory,
                validation,
                Clock.systemUTC()
        );
    }

    McpControlPlaneService(
            McpServerRepository servers,
            JdbcMcpCapabilityDraftStore capabilities,
            JdbcMcpRemoteProviderStore remote,
            JdbcMcpArtifactMetadataStore artifacts,
            McpAppArtifactStore.Writer artifactWriter,
            McpAppArtifactStore.Reader artifactReader,
            McpAppSecurityValidator appSecurity,
            JdbcMcpTaskStore tasks,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            McpReleaseContentFactory contentFactory,
            McpValidationService validation,
            Clock clock) {
        this.servers = servers;
        this.capabilities = capabilities;
        this.remote = remote;
        this.artifacts = artifacts;
        this.artifactWriter = artifactWriter;
        this.artifactReader = artifactReader;
        this.appSecurity = appSecurity;
        this.tasks = tasks;
        this.drafts = drafts;
        this.idempotency = idempotency;
        this.audits = audits;
        this.contentFactory = contentFactory;
        this.validation = validation;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ServerView> listServers(String gatewayGroupId) {
        return servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        required(gatewayGroupId, "gatewayGroupId")
                )
                .stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServerView getServer(String id) {
        return view(requiredServer(id));
    }

    @Transactional
    public MutationResult createServer(
            ServerMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("CREATE_SERVER", command);
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requireCreateRevision(command.expectedRevision());
        Instant now = clock.instant();
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        String id = UuidV7.simpleString();
        servers.saveAndFlush(new McpServerEntity(
                id,
                command.gatewayGroupId(),
                command.serverCode(),
                command.displayName(),
                command.description(),
                command.instructions(),
                command.dialects(),
                command.resourceUri(),
                command.listCacheTtlSeconds(),
                actor,
                now
        ));
        return finish(
                gatewayDraft,
                id,
                0,
                "MCP_SERVER",
                "CREATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("serverCode", command.serverCode()),
                now
        );
    }

    @Transactional
    public MutationResult updateServer(
            String id,
            ServerMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("UPDATE_SERVER", Map.of(
                "id", id,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpServerEntity server = requiredServer(id);
        requireGroup(command.gatewayGroupId(), server.getGatewayGroupId());
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        server.update(
                command.displayName(),
                command.description(),
                command.instructions(),
                command.dialects(),
                command.resourceUri(),
                command.listCacheTtlSeconds(),
                command.enabled(),
                command.expectedRevision(),
                actor,
                now
        );
        servers.flush();
        return finish(
                gatewayDraft,
                id,
                command.expectedRevision() + 1,
                "MCP_SERVER",
                "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("serverCode", server.getServerCode()),
                now
        );
    }

    @Transactional
    public MutationResult deleteServer(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_SERVER", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpServerEntity server = requiredServer(id);
        requireGroup(control.gatewayGroupId(), server.getGatewayGroupId());
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        server.softDelete(control.expectedRevision(), actor, now);
        servers.flush();
        return finish(
                gatewayDraft,
                id,
                control.expectedRevision() + 1,
                "MCP_SERVER",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("serverCode", server.getServerCode()),
                now
        );
    }

    @Transactional(readOnly = true)
    public List<JdbcMcpCapabilityDraftStore.CapabilityDraft> capabilities(
            String gatewayGroupId,
            String serverId,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind) {
        requiredServerInGroup(serverId, gatewayGroupId);
        return capabilities.load(gatewayGroupId)
                .capabilities(kind)
                .stream()
                .filter(item -> item.serverId().equals(serverId))
                .toList();
    }

    @Transactional
    public MutationResult putCapability(
            String id,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind,
            CapabilityMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_" + kind, Map.of(
                "id", resourceId,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        if (id != null) {
            var existing = requiredCapability(
                    command.gatewayGroupId(),
                    kind,
                    id
            );
            if (!existing.serverId().equals(command.serverId())) {
                throw new IllegalArgumentException(
                        "MCP capability cannot move between Servers"
                );
            }
        }
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = capabilities.save(
                new JdbcMcpCapabilityDraftStore.CapabilityDraft(
                        kind,
                        resourceId,
                        command.gatewayGroupId(),
                        command.serverId(),
                        command.name(),
                        command.content(),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                resourceId,
                mutation.revision(),
                "MCP_" + kind,
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
    public MutationResult deleteCapability(
            String id,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_" + kind, Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredCapability(control.gatewayGroupId(), kind, id);
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = capabilities.softDelete(
                kind,
                id,
                control.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                id,
                mutation.revision(),
                "MCP_" + kind,
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(),
                now
        );
    }

    @Transactional(readOnly = true)
    public Preview preview(String gatewayGroupId) {
        McpRuleContent content = contentFactory.preview(gatewayGroupId);
        return new Preview(content, validation.validate(content));
    }

    @Transactional(readOnly = true)
    public McpValidationService.ValidationReport validate(
            String gatewayGroupId) {
        return validation.validate(contentFactory.preview(gatewayGroupId));
    }

    @Transactional(readOnly = true)
    public List<JdbcMcpRemoteProviderStore.RemoteProviderDraft> providers(
            String gatewayGroupId) {
        return remote.providers(gatewayGroupId);
    }

    @Transactional
    public MutationResult putProvider(
            String id,
            RemoteProviderMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_REMOTE_PROVIDER", Map.of(
                "id", resourceId,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        if (id != null) {
            requiredProvider(command.gatewayGroupId(), id);
        }
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.saveProvider(
                new JdbcMcpRemoteProviderStore.RemoteProviderDraft(
                        resourceId,
                        command.gatewayGroupId(),
                        command.providerCode(),
                        command.content(),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                resourceId,
                mutation.revision(),
                "MCP_REMOTE_PROVIDER",
                id == null ? "CREATE" : "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("providerCode", command.providerCode()),
                now
        );
    }

    @Transactional
    public MutationResult deleteProvider(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_PROVIDER", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredProvider(control.gatewayGroupId(), id);
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.softDeleteProvider(
                id,
                control.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                id,
                mutation.revision(),
                "MCP_REMOTE_PROVIDER",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(),
                now
        );
    }

    @Transactional(readOnly = true)
    public List<JdbcMcpRemoteProviderStore.RemoteCapability>
            remoteCapabilities(String providerId) {
        return remote.capabilities(providerId);
    }

    @Transactional(readOnly = true)
    public List<JdbcMcpRemoteProviderStore.RemoteMountDraft> mounts(
            String gatewayGroupId) {
        return remote.mounts(gatewayGroupId);
    }

    @Transactional
    public MutationResult putMount(
            String id,
            RemoteMountMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_REMOTE_MOUNT", Map.of(
                "id", resourceId,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        requiredProvider(command.gatewayGroupId(), command.providerId());
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.saveMount(
                new JdbcMcpRemoteProviderStore.RemoteMountDraft(
                        resourceId,
                        command.gatewayGroupId(),
                        command.serverId(),
                        command.providerId(),
                        command.namespace(),
                        command.capabilityFingerprint(),
                        command.content(),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                resourceId,
                mutation.revision(),
                "MCP_REMOTE_MOUNT",
                id == null ? "CREATE" : "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("namespace", command.namespace()),
                now
        );
    }

    @Transactional
    public MutationResult deleteMount(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_MOUNT", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        boolean exists = remote.mounts(control.gatewayGroupId()).stream()
                .anyMatch(item -> item.id().equals(id));
        if (!exists) {
            throw notFound("MCP Remote Mount", id);
        }
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.softDeleteMount(
                id,
                control.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                id,
                mutation.revision(),
                "MCP_REMOTE_MOUNT",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(),
                now
        );
    }

    @Transactional(readOnly = true)
    public List<JdbcMcpArtifactMetadataStore.ArtifactMetadata> artifacts(
            String gatewayGroupId) {
        return artifacts.list(gatewayGroupId);
    }

    @Transactional(readOnly = true)
    public JdbcMcpArtifactMetadataStore.ArtifactMetadata artifact(String id) {
        return artifacts.find(id).orElseThrow(() -> notFound(
                "MCP App artifact",
                id
        ));
    }

    @Transactional
    public MutationResult uploadArtifact(
            ArtifactUpload command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        byte[] content = command.content();
        String digest = sha256(content);
        var artifactContent = new McpAppArtifactStore.ArtifactContent(
                content,
                digest,
                content.length
        );
        ArtifactMutation mutation = new ArtifactMutation(
                command.gatewayGroupId(),
                command.appCode(),
                command.version(),
                command.displayName(),
                command.resourceUri(),
                "apps/" + command.appCode() + "/" + command.version()
                        + "/index.html",
                digest,
                content.length,
                command.mimeType(),
                command.contentSecurityPolicy(),
                command.permissions(),
                command.allowedOrigins(),
                command.expectedRevision(),
                command.expectedDraftRevision(),
                command.changeReason()
        );
        validateArtifact(mutation, artifactContent);
        McpAppArtifactStore.StoredArtifact stored = artifactWriter.write(
                new McpAppArtifactStore.WriteRequest(
                        command.appCode(),
                        command.version(),
                        content,
                        digest
                )
        );
        return registerArtifact(new ArtifactMutation(
                mutation.gatewayGroupId(),
                mutation.appCode(),
                mutation.version(),
                mutation.displayName(),
                mutation.resourceUri(),
                stored.artifactReference(),
                stored.sha256(),
                stored.sizeBytes(),
                mutation.mimeType(),
                mutation.contentSecurityPolicy(),
                mutation.permissions(),
                mutation.allowedOrigins(),
                mutation.expectedRevision(),
                mutation.expectedDraftRevision(),
                mutation.changeReason()
        ), idempotencyKey, actor, request);
    }

    @Transactional
    public MutationResult registerArtifact(
            ArtifactMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("REGISTER_ARTIFACT", command);
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requireCreateRevision(command.expectedRevision());
        McpAppArtifactStore.ArtifactContent artifactContent =
                artifactReader.read(new McpAppArtifactStore.ReadRequest(
                        command.artifactReference(),
                        command.sha256(),
                        command.sizeBytes()
                ));
        validateArtifact(command, artifactContent);
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        String id = UuidV7.simpleString();
        artifacts.save(new JdbcMcpArtifactMetadataStore.ArtifactMetadata(
                id,
                command.gatewayGroupId(),
                command.appCode(),
                command.version(),
                command.displayName(),
                command.resourceUri(),
                command.artifactReference(),
                command.sha256(),
                command.sizeBytes(),
                command.mimeType(),
                command.contentSecurityPolicy(),
                command.permissions(),
                command.allowedOrigins(),
                actor.actorId(),
                now
        ));
        return finish(
                gatewayDraft,
                id,
                0,
                "MCP_APP_ARTIFACT",
                "REGISTER",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(
                        "appCode", command.appCode(),
                        "version", command.version(),
                        "sha256", command.sha256()
                ),
                now
        );
    }

    private void validateArtifact(
            ArtifactMutation command,
            McpAppArtifactStore.ArtifactContent artifact) {
        String serverCode;
        try {
            serverCode = URI.create(command.resourceUri()).getRawAuthority();
        } catch (IllegalArgumentException failure) {
            throw new McpAppArtifactStore.ArtifactRejectedException(
                    "MCP App resource URI is invalid",
                    failure
            );
        }
        try {
            appSecurity.validate(new McpAppSecurityValidator.Manifest(
                    serverCode,
                    command.appCode(),
                    command.version(),
                    command.resourceUri(),
                    command.sha256(),
                    command.sizeBytes(),
                    command.mimeType(),
                    command.contentSecurityPolicy(),
                    command.permissions(),
                    command.allowedOrigins()
            ), artifact);
        } catch (McpProtocolException failure) {
            throw new McpAppArtifactStore.ArtifactRejectedException(
                    failure.getMessage(),
                    failure
            );
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    @Transactional
    public MutationResult revokeArtifact(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("REVOKE_ARTIFACT", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        JdbcMcpArtifactMetadataStore.ArtifactMetadata artifact = artifact(id);
        requireGroup(control.gatewayGroupId(), artifact.gatewayGroupId());
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        if (!artifacts.revoke(id)) {
            throw new IllegalStateException(
                    "GATEWAY_MCP_ARTIFACT_ALREADY_REVOKED"
            );
        }
        Instant now = clock.instant();
        return finish(
                gatewayDraft,
                id,
                0,
                "MCP_APP_ARTIFACT",
                "REVOKE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(
                        "appCode", artifact.appCode(),
                        "version", artifact.version(),
                        "sha256", artifact.sha256()
                ),
                now
        );
    }

    @Transactional(readOnly = true)
    public List<JdbcMcpTaskStore.TaskRecord> tasks(
            String tenantId,
            String clientId) {
        return tasks.list(required(tenantId, "tenantId"), clientId);
    }

    @Transactional(readOnly = true)
    public JdbcMcpTaskStore.TaskRecord task(String id) {
        return tasks.find(id).orElseThrow(() -> notFound("MCP Task", id));
    }

    @Transactional
    public boolean cancelTask(
            String id,
            long expectedRevision,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String scopeId = "TASK:" + required(id, "id");
        String digest = digest("CANCEL_TASK", Map.of(
                "id", id,
                "expectedRevision", expectedRevision
        ));
        Boolean replay = replayTask(scopeId, idempotencyKey, digest);
        if (replay != null) {
            return replay;
        }
        JdbcMcpTaskStore.TaskRecord task = task(id);
        Instant now = clock.instant();
        boolean cancelled = tasks.cancel(id, expectedRevision, now);
        if (!cancelled) {
            return false;
        }
        audit(
                actor,
                request,
                "MCP_TASK",
                id,
                "CANCEL",
                Map.of(
                        "tenantId", task.tenantId(),
                        "serverCode", task.serverCode(),
                        "toolName", task.toolName()
                ),
                now
        );
        idempotency.save(new IdempotencyStore.Record(
                TASK_IDEMPOTENCY_SCOPE,
                scopeId,
                required(idempotencyKey, "idempotencyKey"),
                digest,
                id,
                Map.of("cancelled", true),
                now,
                now.plus(Duration.ofDays(7))
        ));
        return true;
    }

    private Boolean replayTask(
            String scopeId,
            String idempotencyKey,
            String payloadDigest) {
        String key = required(idempotencyKey, "idempotencyKey");
        IdempotencyStore.Record existing = idempotency.find(
                TASK_IDEMPOTENCY_SCOPE,
                scopeId,
                key
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(payloadDigest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        return Boolean.TRUE.equals(existing.response().get("cancelled"));
    }

    private MutationResult finish(
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
        MutationResult result = new MutationResult(
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
        audit(
                actor,
                request,
                resourceType,
                resourceId,
                action,
                summary,
                now
        );
        return result;
    }

    private MutationResult replay(
            String gatewayGroupId,
            String idempotencyKey,
            String payloadDigest) {
        String key = required(idempotencyKey, "idempotencyKey");
        IdempotencyStore.Record existing = idempotency.find(
                IDEMPOTENCY_SCOPE,
                gatewayGroupId,
                key
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(payloadDigest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        return new MutationResult(
                number(existing.response(), "draftRevision"),
                existing.resourceId(),
                number(existing.response(), "resourceRevision"),
                true
        );
    }

    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> summary,
            Instant now) {
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
    }

    private McpServerEntity requiredServer(String id) {
        return servers.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("MCP Server", id));
    }

    private void requiredServerInGroup(String id, String gatewayGroupId) {
        requireGroup(gatewayGroupId, requiredServer(id).getGatewayGroupId());
    }

    private JdbcMcpCapabilityDraftStore.CapabilityDraft requiredCapability(
            String gatewayGroupId,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind,
            String id) {
        return capabilities.load(gatewayGroupId).capabilities(kind)
                .stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("MCP " + kind, id));
    }

    private JdbcMcpRemoteProviderStore.RemoteProviderDraft requiredProvider(
            String gatewayGroupId,
            String id) {
        return remote.providers(gatewayGroupId).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Remote Provider", id));
    }

    private GatewayDraftEntity editable(
            String gatewayGroupId,
            long expectedRevision) {
        GatewayDraftEntity draft = drafts.findById(gatewayGroupId)
                .orElseThrow(() -> notFound(
                        "Gateway Draft",
                        gatewayGroupId
                ));
        draft.assertEditable(expectedRevision);
        return draft;
    }

    private void requireGroup(String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(
                    "MCP resource belongs to another Gateway Group"
            );
        }
    }

    private void requireCreateRevision(long expectedRevision) {
        if (expectedRevision != 0) {
            throw new GatewayAdminRevisionConflictException(0);
        }
    }

    private String digest(String action, Object command) {
        return GatewayRuleCanonicalizer.sha256(canonicalizer.canonicalBytes(
                Map.of("action", action, "command", command)
        ));
    }

    private long number(Map<String, Object> value, String key) {
        Object item = value.get(key);
        return item instanceof Number number
                ? number.longValue()
                : Long.parseLong(item.toString());
    }

    private GatewayAdminNotFoundException notFound(
            String resource,
            String id) {
        return new GatewayAdminNotFoundException(
                resource + " " + id + " was not found"
        );
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private ServerView view(McpServerEntity server) {
        return new ServerView(
                server.getId(),
                server.getGatewayGroupId(),
                server.getServerCode(),
                server.getDisplayName(),
                server.getDescription(),
                server.getInstructions(),
                server.getDialects(),
                server.getResourceUri(),
                server.getListCacheTtlSeconds(),
                server.isEnabled(),
                server.getRevision(),
                server.getCreatedAt(),
                server.getUpdatedAt()
        );
    }

    public record ServerMutation(
            String gatewayGroupId,
            String serverCode,
            String displayName,
            String description,
            String instructions,
            Set<String> dialects,
            String resourceUri,
            long listCacheTtlSeconds,
            boolean enabled,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record CapabilityMutation(
            String gatewayGroupId,
            String serverId,
            String name,
            Map<String, Object> content,
            boolean enabled,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record RemoteProviderMutation(
            String gatewayGroupId,
            String providerCode,
            Map<String, Object> content,
            boolean enabled,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record RemoteMountMutation(
            String gatewayGroupId,
            String serverId,
            String providerId,
            String namespace,
            String capabilityFingerprint,
            Map<String, Object> content,
            boolean enabled,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record ArtifactMutation(
            String gatewayGroupId,
            String appCode,
            String version,
            String displayName,
            String resourceUri,
            String artifactReference,
            String sha256,
            long sizeBytes,
            String mimeType,
            String contentSecurityPolicy,
            Set<String> permissions,
            Set<String> allowedOrigins,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record ArtifactUpload(
            String gatewayGroupId,
            String appCode,
            String version,
            String displayName,
            String resourceUri,
            String mimeType,
            String contentSecurityPolicy,
            Set<String> permissions,
            Set<String> allowedOrigins,
            byte[] content,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {

        public ArtifactUpload {
            content = Objects.requireNonNull(content, "content").clone();
            permissions = Set.copyOf(permissions);
            allowedOrigins = Set.copyOf(allowedOrigins);
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    public record MutationControl(
            String gatewayGroupId,
            long expectedRevision,
            long expectedDraftRevision,
            String changeReason
    ) {
    }

    public record ServerView(
            String id,
            String gatewayGroupId,
            String serverCode,
            String displayName,
            String description,
            String instructions,
            Set<String> dialects,
            String resourceUri,
            long listCacheTtlSeconds,
            boolean enabled,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MutationResult(
            long draftRevision,
            String resourceId,
            long resourceRevision,
            boolean replayed
    ) {
    }

    public record Preview(
            McpRuleContent content,
            McpValidationService.ValidationReport validation
    ) {
    }
}
