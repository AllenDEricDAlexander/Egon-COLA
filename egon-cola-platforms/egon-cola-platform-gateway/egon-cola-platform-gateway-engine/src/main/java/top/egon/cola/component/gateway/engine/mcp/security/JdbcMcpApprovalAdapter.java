package top.egon.cola.component.gateway.engine.mcp.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class JdbcMcpApprovalAdapter implements McpApprovalPort {

    private final DataSource dataSource;
    private final Clock clock;

    public JdbcMcpApprovalAdapter(DataSource dataSource, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Publisher<Result> consume(ConsumptionRequest request) {
        Objects.requireNonNull(request, "request");
        return Mono.fromCallable(() -> consumeBlocking(request))
                .onErrorReturn(SQLException.class, Result.UNAVAILABLE)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Result consumeBlocking(ConsumptionRequest request)
            throws SQLException {
        Instant now = clock.instant();
        try (Connection connection = dataSource.getConnection()) {
            ApprovalRow row = find(connection, request.tokenDigest());
            if (row == null || !row.matches(request) || !row.validAt(now)) {
                return Result.MISMATCH;
            }
            if ("CONSUMED".equals(row.status())) {
                return Result.CONSUMED;
            }
            if (!"PENDING".equals(row.status())) {
                return Result.MISMATCH;
            }
            if (consume(connection, request, now) == 1) {
                return Result.APPROVED;
            }
            ApprovalRow raced = find(connection, request.tokenDigest());
            return raced != null && "CONSUMED".equals(raced.status())
                    ? Result.CONSUMED
                    : Result.MISMATCH;
        }
    }

    private ApprovalRow find(Connection connection, String tokenDigest)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT subject_id, tenant_id, client_id, server_code,
                       tool_name, argument_digest, status, expires_at
                  FROM gateway_mcp_approval
                 WHERE token_digest = ?
                """)) {
            statement.setString(1, tokenDigest);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new ApprovalRow(
                        result.getString("subject_id"),
                        result.getString("tenant_id"),
                        result.getString("client_id"),
                        result.getString("server_code"),
                        result.getString("tool_name"),
                        result.getString("argument_digest"),
                        result.getString("status"),
                        result.getTimestamp("expires_at").toInstant()
                );
            }
        }
    }

    private int consume(
            Connection connection,
            ConsumptionRequest request,
            Instant now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
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
                """)) {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setString(2, request.tokenDigest());
            statement.setString(3, request.subjectId());
            statement.setString(4, request.tenantId());
            statement.setString(5, request.clientId());
            statement.setString(6, request.serverCode());
            statement.setString(7, request.toolName());
            statement.setString(8, request.argumentDigest());
            statement.setTimestamp(9, Timestamp.from(now));
            return statement.executeUpdate();
        }
    }

    private record ApprovalRow(
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            String status,
            Instant expiresAt
    ) {

        private boolean matches(ConsumptionRequest request) {
            return subjectId.equals(request.subjectId())
                    && tenantId.equals(request.tenantId())
                    && clientId.equals(request.clientId())
                    && serverCode.equals(request.serverCode())
                    && toolName.equals(request.toolName())
                    && argumentDigest.equals(request.argumentDigest());
        }

        private boolean validAt(Instant now) {
            return expiresAt.isAfter(now);
        }
    }
}
