package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Runtime observability facade; recovery is addressable only by mutation id.
 */
public final class RuntimeQueryService {

    private final ControlPlaneRuntimeStatusPort statusPort;
    private final MutationQueryPort mutationQueryPort;
    private final MutationRecoveryPort recoveryPort;

    public RuntimeQueryService(
            ControlPlaneRuntimeStatusPort statusPort,
            MutationQueryPort mutationQueryPort,
            MutationRecoveryPort recoveryPort) {
        this.statusPort = Objects.requireNonNull(statusPort, "statusPort");
        this.mutationQueryPort = Objects.requireNonNull(
                mutationQueryPort, "mutationQueryPort");
        this.recoveryPort = Objects.requireNonNull(recoveryPort, "recoveryPort");
    }

    public ControlPlaneRuntimeStatusPort.RuntimeStatus status() {
        return statusPort.status();
    }

    public ControlPlaneRuntimeStatusPort.RuntimeStatus gatewayDdcStatus() {
        return statusPort.status();
    }

    public MutationPage mutations(
            String tenantId,
            String status,
            String cursor,
            int pageSize) {
        require(tenantId, "tenantId");
        if (pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("pageSize must be between 1 and 200");
        }
        return mutationQueryPort.query(tenantId, status, cursor, pageSize);
    }

    public RetryResult retry(
            String tenantId,
            String mutationId,
            String actorId) {
        return recoveryPort.retry(
                require(tenantId, "tenantId"),
                require(mutationId, "mutationId"),
                require(actorId, "actorId"));
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface MutationQueryPort {
        MutationPage query(
                String tenantId,
                String status,
                String cursor,
                int pageSize);
    }

    @FunctionalInterface
    public interface MutationRecoveryPort {
        RetryResult retry(String tenantId, String mutationId, String actorId);
    }

    public record MutationView(
            String mutationId,
            String scopeType,
            String scopeId,
            String commandId,
            String status,
            int attempt,
            String lastErrorCode,
            Instant updatedAt) {
    }

    public record MutationPage(List<MutationView> items, String nextCursor) {
        public MutationPage {
            items = List.copyOf(items);
        }
    }

    public record RetryResult(String mutationId, String status) {
    }
}
