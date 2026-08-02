package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class JdbcMcpRemoteProviderStore {

    private final JdbcTemplate jdbc;

    private final McpJdbcJson json;

    public JdbcMcpRemoteProviderStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    public List<RemoteProviderDraft> providers(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, provider_code, display_name,
                       dialect, transport_type, endpoint_reference,
                       auth_profile_reference, tls_profile_reference,
                       capability_fingerprint, status, enabled, revision
                  FROM gateway_mcp_remote_provider
                 WHERE gateway_group_id = ?
                   AND deleted = FALSE
                 ORDER BY provider_code
                """, (result, row) -> {
            Map<String, Object> content = new java.util.LinkedHashMap<>();
            content.put("displayName", result.getString("display_name"));
            content.put("dialect", result.getString("dialect"));
            content.put("transportType", result.getString("transport_type"));
            content.put(
                    "endpointReference",
                    result.getString("endpoint_reference")
            );
            put(content, "authProfileReference", result.getString(
                    "auth_profile_reference"
            ));
            put(content, "tlsProfileReference", result.getString(
                    "tls_profile_reference"
            ));
            put(content, "capabilityFingerprint", result.getString(
                    "capability_fingerprint"
            ));
            content.put("status", result.getString("status"));
            return new RemoteProviderDraft(
                    result.getString("id"),
                    result.getString("gateway_group_id"),
                    result.getString("provider_code"),
                    content,
                    result.getBoolean("enabled"),
                    result.getLong("revision")
            );
        }, gatewayGroupId);
    }

    public Mutation saveProvider(
            RemoteProviderDraft provider,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateRevision(expectedRevision);
        Map<String, Object> content = provider.content();
        Object[] mutable = providerValues(provider, content, actor, now);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_remote_provider
                   SET provider_code = ?, display_name = ?, dialect = ?,
                       transport_type = ?, endpoint_reference = ?,
                       auth_profile_reference = ?, tls_profile_reference = ?,
                       capability_fingerprint = ?, status = ?, enabled = ?,
                       revision = revision + 1,
                       updated_at = ?, updated_by = ?
                 WHERE id = ? AND revision = ? AND deleted = FALSE
                """, append(mutable, provider.id(), expectedRevision));
        if (updated == 1) {
            return new Mutation(provider.id(), expectedRevision + 1);
        }
        Long currentRevision = currentRevision(
                "gateway_mcp_remote_provider",
                provider.id()
        );
        if (currentRevision != null || expectedRevision != 0) {
            throw revisionConflict(currentRevision);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_provider(
                    id, gateway_group_id, provider_code, display_name,
                    dialect, transport_type, endpoint_reference,
                    auth_profile_reference, tls_profile_reference,
                    capability_fingerprint, status, enabled,
                    revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    0, FALSE, ?, ?, ?, ?
                )
                """,
                provider.id(),
                provider.gatewayGroupId(),
                provider.providerCode(),
                required(content, "displayName"),
                required(content, "dialect"),
                required(content, "transportType"),
                required(content, "endpointReference"),
                optional(content, "authProfileReference"),
                optional(content, "tlsProfileReference"),
                optional(content, "capabilityFingerprint"),
                content.getOrDefault("status", "CONFIGURED").toString(),
                provider.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new Mutation(provider.id(), 0);
    }

    public List<RemoteCapability> capabilities(String providerId) {
        return jdbc.query("""
                SELECT id, provider_id, primitive_type, remote_name,
                       descriptor::text AS descriptor,
                       capability_fingerprint, synced_at
                  FROM gateway_mcp_remote_capability
                 WHERE provider_id = ?
                 ORDER BY primitive_type, remote_name
                """, (result, row) -> new RemoteCapability(
                result.getString("id"),
                result.getString("provider_id"),
                result.getString("primitive_type"),
                result.getString("remote_name"),
                json.map(result.getString("descriptor")),
                result.getString("capability_fingerprint"),
                result.getTimestamp("synced_at").toInstant()
        ), providerId);
    }

    @Transactional
    public void replaceCapabilities(
            String providerId,
            String fingerprint,
            List<RemoteCapability> capabilities,
            Instant syncedAt) {
        jdbc.update(
                "DELETE FROM gateway_mcp_remote_capability "
                        + "WHERE provider_id = ?",
                providerId
        );
        for (RemoteCapability capability : capabilities) {
            jdbc.update("""
                    INSERT INTO gateway_mcp_remote_capability(
                        id, provider_id, primitive_type, remote_name,
                        descriptor, capability_fingerprint, synced_at
                    ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?)
                    """,
                    capability.id(),
                    providerId,
                    capability.primitiveType(),
                    capability.remoteName(),
                    json.write(capability.descriptor()),
                    fingerprint,
                    McpJdbcJson.timestamp(syncedAt)
            );
        }
        jdbc.update("""
                UPDATE gateway_mcp_remote_provider
                   SET capability_fingerprint = ?,
                       status = 'SYNCED',
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ? AND deleted = FALSE
                """, fingerprint, McpJdbcJson.timestamp(syncedAt), providerId);
    }

    public List<RemoteMountDraft> mounts(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, server_id, provider_id,
                       namespace, capability_fingerprint,
                       content::text AS content, enabled, revision
                  FROM gateway_mcp_remote_mount_draft
                 WHERE gateway_group_id = ? AND deleted = FALSE
                 ORDER BY server_id, namespace
                """, (result, row) -> new RemoteMountDraft(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getString("server_id"),
                result.getString("provider_id"),
                result.getString("namespace"),
                result.getString("capability_fingerprint"),
                json.map(result.getString("content")),
                result.getBoolean("enabled"),
                result.getLong("revision")
        ), gatewayGroupId);
    }

    public Mutation saveMount(
            RemoteMountDraft mount,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_remote_mount_draft
                   SET server_id = ?, provider_id = ?, namespace = ?,
                       capability_fingerprint = ?, content = ?::jsonb,
                       enabled = ?, revision = revision + 1,
                       updated_at = ?, updated_by = ?
                 WHERE id = ? AND revision = ? AND deleted = FALSE
                """,
                mount.serverId(),
                mount.providerId(),
                mount.namespace(),
                mount.capabilityFingerprint(),
                json.write(mount.content()),
                mount.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                mount.id(),
                expectedRevision
        );
        if (updated == 1) {
            return new Mutation(mount.id(), expectedRevision + 1);
        }
        Long currentRevision = currentRevision(
                "gateway_mcp_remote_mount_draft",
                mount.id()
        );
        if (currentRevision != null || expectedRevision != 0) {
            throw revisionConflict(currentRevision);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_mount_draft(
                    id, gateway_group_id, server_id, provider_id, namespace,
                    capability_fingerprint, content, enabled,
                    revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?::jsonb, ?,
                    0, FALSE, ?, ?, ?, ?
                )
                """,
                mount.id(),
                mount.gatewayGroupId(),
                mount.serverId(),
                mount.providerId(),
                mount.namespace(),
                mount.capabilityFingerprint(),
                json.write(mount.content()),
                mount.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new Mutation(mount.id(), 0);
    }

    private Object[] providerValues(
            RemoteProviderDraft provider,
            Map<String, Object> content,
            AdminActor actor,
            Instant now) {
        return new Object[]{
                provider.providerCode(),
                required(content, "displayName"),
                required(content, "dialect"),
                required(content, "transportType"),
                required(content, "endpointReference"),
                optional(content, "authProfileReference"),
                optional(content, "tlsProfileReference"),
                optional(content, "capabilityFingerprint"),
                content.getOrDefault("status", "CONFIGURED").toString(),
                provider.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        };
    }

    private Object[] append(Object[] values, Object... tail) {
        Object[] result = java.util.Arrays.copyOf(
                values,
                values.length + tail.length
        );
        System.arraycopy(tail, 0, result, values.length, tail.length);
        return result;
    }

    private Long currentRevision(
            String table,
            String id) {
        List<Long> values = jdbc.query(
                "SELECT revision FROM " + table + " WHERE id = ?",
                (result, row) -> result.getLong("revision"),
                id
        );
        return values.stream().findFirst().orElse(null);
    }

    private GatewayAdminRevisionConflictException revisionConflict(
            Long current) {
        return new GatewayAdminRevisionConflictException(
                current == null ? -1 : current
        );
    }

    private String required(Map<String, Object> content, String name) {
        Object value = content.get(name);
        return McpJdbcJson.required(
                value == null ? null : value.toString(),
                name
        );
    }

    private String optional(Map<String, Object> content, String name) {
        Object value = content.get(name);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().trim();
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private void validateRevision(long revision) {
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    public record RemoteProviderDraft(
            String id,
            String gatewayGroupId,
            String providerCode,
            Map<String, Object> content,
            boolean enabled,
            long revision
    ) {

        public RemoteProviderDraft {
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            providerCode = McpJdbcJson.required(
                    providerCode,
                    "providerCode"
            );
            content = Map.copyOf(Objects.requireNonNull(content, "content"));
        }
    }

    public record RemoteCapability(
            String id,
            String providerId,
            String primitiveType,
            String remoteName,
            Map<String, Object> descriptor,
            String capabilityFingerprint,
            Instant syncedAt
    ) {
    }

    public record RemoteMountDraft(
            String id,
            String gatewayGroupId,
            String serverId,
            String providerId,
            String namespace,
            String capabilityFingerprint,
            Map<String, Object> content,
            boolean enabled,
            long revision
    ) {

        public RemoteMountDraft {
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            serverId = McpJdbcJson.required(serverId, "serverId");
            providerId = McpJdbcJson.required(providerId, "providerId");
            namespace = McpJdbcJson.required(namespace, "namespace");
            capabilityFingerprint = McpJdbcJson.required(
                    capabilityFingerprint,
                    "capabilityFingerprint"
            );
            content = Map.copyOf(Objects.requireNonNull(content, "content"));
        }
    }

    public record Mutation(String id, long revision) {
    }
}
