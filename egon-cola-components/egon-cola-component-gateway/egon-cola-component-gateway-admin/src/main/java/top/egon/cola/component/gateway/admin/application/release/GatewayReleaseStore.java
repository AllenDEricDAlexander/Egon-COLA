package top.egon.cola.component.gateway.admin.application.release;

import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GatewayReleaseStore {

    void insert(
            ReleaseRecord release,
            CompiledGatewayRelease compiled,
            int attemptNo);

    Optional<ReleaseRecord> find(String releaseId);

    List<ReleaseRecord> history(String gatewayGroupId);

    List<RecoverableAttempt> recoverable();

    List<AttemptRecord> attempts(String releaseId);

    int latestAttempt(String releaseId);

    CompiledGatewayRelease loadCompiled(String releaseId);

    int nextAttempt(String releaseId, Instant now);

    void beginAttempt(String releaseId, int attemptNo, Instant now);

    void completeAttempt(
            String releaseId,
            int attemptNo,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            String errorCode,
            String errorMessage,
            List<TargetRecord> targets,
            Instant now);

    boolean hasReleaseInProgress(String gatewayGroupId);

    record ReleaseRecord(
            String id,
            String gatewayGroupId,
            long draftRevision,
            String basedOnReleaseId,
            String rollbackOfReleaseId,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            Map<String, Object> validationReport,
            Map<String, Object> structuredDiff,
            String changeReason,
            Instant createdAt,
            String createdBy,
            Instant updatedAt
    ) {
    }

    record TargetRecord(
            String instanceId,
            String leaseId,
            String status,
            Long appliedVersion,
            String appliedArtifactSha256,
            String errorCode,
            Instant observedAt
    ) {
    }

    record AttemptRecord(
            int attemptNo,
            String status,
            String changeId,
            Instant startedAt,
            Instant completedAt,
            String errorCode,
            String errorMessage,
            List<TargetRecord> targets
    ) {
    }

    record RecoverableAttempt(
            String releaseId,
            String gatewayGroupId,
            int attemptNo
    ) {
    }
}
