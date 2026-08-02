package top.egon.cola.component.gateway.admin.mcp.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcMcpApprovalStore {

    private final JdbcTemplate jdbc;

    public JdbcMcpApprovalStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public void issue(Approval approval) {
        Objects.requireNonNull(approval, "approval");
        jdbc.update("""
                INSERT INTO gateway_mcp_approval(
                    id, token_digest, subject_id, tenant_id, client_id,
                    server_code, tool_name, argument_digest, status,
                    revision, issued_at, expires_at, consumed_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, ?, NULL
                )
                """,
                approval.id(),
                approval.tokenDigest(),
                approval.subjectId(),
                approval.tenantId(),
                approval.clientId(),
                approval.serverCode(),
                approval.toolName(),
                approval.argumentDigest(),
                McpJdbcJson.timestamp(approval.issuedAt()),
                McpJdbcJson.timestamp(approval.expiresAt())
        );
    }

    public boolean consume(
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'CONSUMED', consumed_at = ?,
                       revision = revision + 1
                 WHERE token_digest = ?
                   AND subject_id = ?
                   AND tenant_id = ?
                   AND client_id = ?
                   AND server_code = ?
                   AND tool_name = ?
                   AND argument_digest = ?
                   AND status = 'PENDING'
                   AND expires_at > ?
                """,
                McpJdbcJson.timestamp(now),
                digest(tokenDigest, "tokenDigest"),
                McpJdbcJson.required(subjectId, "subjectId"),
                McpJdbcJson.required(tenantId, "tenantId"),
                McpJdbcJson.required(clientId, "clientId"),
                McpJdbcJson.required(serverCode, "serverCode"),
                McpJdbcJson.required(toolName, "toolName"),
                digest(argumentDigest, "argumentDigest"),
                McpJdbcJson.timestamp(now)
        ) == 1;
    }

    public Optional<Approval> find(String id) {
        List<Approval> values = jdbc.query("""
                SELECT id, token_digest, subject_id, tenant_id, client_id,
                       server_code, tool_name, argument_digest,
                       issued_at, expires_at
                  FROM gateway_mcp_approval
                 WHERE id = ?
                """, (result, row) -> new Approval(
                result.getString("id"),
                result.getString("token_digest"),
                result.getString("subject_id"),
                result.getString("tenant_id"),
                result.getString("client_id"),
                result.getString("server_code"),
                result.getString("tool_name"),
                result.getString("argument_digest"),
                result.getTimestamp("issued_at").toInstant(),
                result.getTimestamp("expires_at").toInstant()
        ), id);
        return values.stream().findFirst();
    }

    public int expire(Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'EXPIRED', revision = revision + 1
                 WHERE status = 'PENDING' AND expires_at <= ?
                """, McpJdbcJson.timestamp(now));
    }

    public boolean revoke(String id, long expectedRevision) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'REVOKED', revision = revision + 1
                 WHERE id = ? AND revision = ? AND status = 'PENDING'
                """, id, expectedRevision) == 1;
    }

    private static String digest(String value, String field) {
        String digest = McpJdbcJson.required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }

    public record Approval(
            String id,
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant issuedAt,
            Instant expiresAt
    ) {

        public Approval {
            id = McpJdbcJson.required(id, "id");
            tokenDigest = digest(tokenDigest, "tokenDigest");
            subjectId = McpJdbcJson.required(subjectId, "subjectId");
            tenantId = McpJdbcJson.required(tenantId, "tenantId");
            clientId = McpJdbcJson.required(clientId, "clientId");
            serverCode = McpJdbcJson.required(serverCode, "serverCode");
            toolName = McpJdbcJson.required(toolName, "toolName");
            argumentDigest = digest(argumentDigest, "argumentDigest");
            issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
            if (!expiresAt.isAfter(issuedAt)) {
                throw new IllegalArgumentException(
                        "expiresAt must be after issuedAt"
                );
            }
        }
    }
}
