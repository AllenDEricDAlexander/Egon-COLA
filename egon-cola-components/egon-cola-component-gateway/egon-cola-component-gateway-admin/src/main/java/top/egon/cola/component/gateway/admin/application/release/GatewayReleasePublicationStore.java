package top.egon.cola.component.gateway.admin.application.release;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GatewayReleasePublicationStore {

    void insertAll(List<PublicationRecord> operations);

    List<PublicationRecord> findAttempt(String releaseId, int attemptNo);

    Optional<PublicationRecord> nextIncomplete(
            String releaseId,
            int attemptNo);

    void resolveVersion(
            String changeId,
            long expectedVersion,
            Instant now);

    void markSubmitted(String changeId, Instant now);

    void markResult(
            String changeId,
            Long targetVersion,
            PublicationStatus status,
            String errorCode,
            String errorMessage,
            Instant now);

    record PublicationRecord(
            String releaseId,
            int attemptNo,
            int phaseOrder,
            PhaseType phaseType,
            String configKey,
            String contentValue,
            String contentSha256,
            Long expectedVersion,
            String changeId,
            Long ddcTargetVersion,
            PublicationStatus status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    enum PhaseType {
        CHUNK,
        ACTIVATION
    }

    enum PublicationStatus {
        PLANNED,
        RESOLVED,
        SUBMITTED,
        SUCCESS,
        FAILED,
        PARTIAL_SUCCESS,
        TIMEOUT,
        UNKNOWN;

        public boolean terminalResult() {
            return switch (this) {
                case SUCCESS, FAILED, PARTIAL_SUCCESS, TIMEOUT, UNKNOWN ->
                        true;
                case PLANNED, RESOLVED, SUBMITTED -> false;
            };
        }
    }
}
