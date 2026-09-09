package top.egon.cola.component.yuheng.admin.openapi.domain.enums;

import java.time.Instant;
import java.util.Objects;

/**
 * Durable per-Group OpenAPI synchronization states.
 *
 * <p>中文：OpenAPI Group 同步持久状态及其允许的 CAS 状态迁移。
 */
public enum GatewayOpenApiSyncStateEnum {

    /** A provider Group was discovered but not claimed. */
    DISCOVERED,

    /** A worker owns a fetch attempt. */
    FETCHING,

    /** The bounded document is being validated and canonicalized. */
    VALIDATING,

    /** The document is retained but cannot be ingested. */
    INVALID,

    /** Same-build manifest or contract drift was detected. */
    INCONSISTENT_BUILD,

    /** Definition ingestion is in progress or awaiting post-commit CAS. */
    INGESTING,

    /** The Group document and aggregate Definition Set are current. */
    VALID,

    /** A definition transaction failed and may be retried. */
    INGEST_FAILED,

    /** A provider fetch failed and may be retried. */
    FETCH_FAILED,

    /** The provider observation is no longer healthy/current. */
    STALE;

    /**
     * Returns whether a state transition is legal for the durable state
     * machine.
     *
     * @param target requested next state
     * @return {@code true} when the transition is allowed
     */
    public boolean canTransitionTo(GatewayOpenApiSyncStateEnum target) {
        Objects.requireNonNull(target, "target");
        return switch (this) {
            case DISCOVERED -> target == FETCHING || target == STALE;
            case FETCHING -> target == VALIDATING
                    || target == FETCH_FAILED
                    || target == STALE;
            case VALIDATING -> target == INVALID
                    || target == INCONSISTENT_BUILD
                    || target == INGESTING
                    || target == STALE;
            case INVALID -> target == STALE;
            case INCONSISTENT_BUILD -> target == STALE;
            case INGESTING -> target == VALID
                    || target == INGEST_FAILED
                    || target == STALE;
            case VALID -> target == STALE;
            case INGEST_FAILED -> target == DISCOVERED
                    || target == FETCHING
                    || target == STALE;
            case FETCH_FAILED -> target == DISCOVERED
                    || target == FETCHING
                    || target == STALE;
            case STALE -> target == DISCOVERED || target == FETCHING;
        };
    }

    /**
     * Returns whether a state can be retried without a new build.
     *
     * @return {@code true} for transient fetch/ingestion failures
     */
    public boolean retryable() {
        return this == FETCH_FAILED || this == INGEST_FAILED;
    }

    /**
     * Returns whether a row may be claimed at the supplied time.
     *
     * @param now current UTC instant
     * @param nextRetryAt next retry instant, or {@code null} for immediate work
     * @return {@code true} when the state is claimable and due
     */
    public boolean claimableAt(Instant now, Instant nextRetryAt) {
        Objects.requireNonNull(now, "now");
        if (this != DISCOVERED && !retryable() && this != STALE) {
            return false;
        }
        return nextRetryAt == null || !nextRetryAt.isAfter(now);
    }
}
