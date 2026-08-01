package top.egon.cola.platform.rbac3.admin.worker;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Provides multi-node event/version ownership for runtime projection delivery.
 */
@Repository
public class RedisProjectionCheckpointStore
        implements RuntimeSnapshotRebuildWorker.ProjectionCheckpointStore {

    private static final Duration CLAIM_TTL = Duration.ofMinutes(5);
    private static final Duration APPLIED_TTL = Duration.ofDays(30);
    private static final Pattern KEY_PART = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final RedissonClient redisson;

    public RedisProjectionCheckpointStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
    }

    @Override
    public RuntimeSnapshotRebuildWorker.Claim claim(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        RBucket<String> bucket = bucket(tenantId, aggregateType, aggregateId);
        String pending = value(eventId, aggregateVersion, "PENDING");
        if (bucket.trySet(
                pending, CLAIM_TTL.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
            return RuntimeSnapshotRebuildWorker.Claim.ACQUIRED;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            String current = bucket.get();
            if (current == null) {
                if (bucket.trySet(
                        pending, CLAIM_TTL.toMillis(),
                        java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    return RuntimeSnapshotRebuildWorker.Claim.ACQUIRED;
                }
                continue;
            }
            Checkpoint parsed = parse(current);
            if (parsed.aggregateVersion() > aggregateVersion) {
                return RuntimeSnapshotRebuildWorker.Claim.STALE;
            }
            if (parsed.aggregateVersion() == aggregateVersion) {
                return "APPLIED".equals(parsed.state())
                        ? RuntimeSnapshotRebuildWorker.Claim.ALREADY_APPLIED
                        : RuntimeSnapshotRebuildWorker.Claim.BUSY;
            }
            if (!"APPLIED".equals(parsed.state())) {
                return RuntimeSnapshotRebuildWorker.Claim.BUSY;
            }
            if (bucket.compareAndSet(current, pending)) {
                bucket.expire(CLAIM_TTL);
                return RuntimeSnapshotRebuildWorker.Claim.ACQUIRED;
            }
        }
        return RuntimeSnapshotRebuildWorker.Claim.BUSY;
    }

    @Override
    public void complete(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        String pending = value(eventId, aggregateVersion, "PENDING");
        String applied = value(eventId, aggregateVersion, "APPLIED");
        RBucket<String> bucket = bucket(tenantId, aggregateType, aggregateId);
        if (bucket.compareAndSet(pending, applied)) {
            bucket.expire(APPLIED_TTL);
            return;
        }
        throw new IllegalStateException("RBAC3_PROJECTION_CHECKPOINT_OWNERSHIP_LOST");
    }

    @Override
    public void release(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        String pending = value(eventId, aggregateVersion, "PENDING");
        bucket(tenantId, aggregateType, aggregateId)
                .compareAndSet(pending, null);
    }

    private RBucket<String> bucket(
            String tenantId,
            String aggregateType,
            String aggregateId) {
        return redisson.getBucket(
                "rbac3:{" + part(tenantId) + "}:projection:"
                        + part(aggregateType) + ':' + part(aggregateId),
                StringCodec.INSTANCE);
    }

    private static String value(String eventId, long version, String state) {
        if (version < 0) {
            throw new IllegalArgumentException("aggregateVersion must not be negative");
        }
        return part(eventId) + '|' + version + '|' + state;
    }

    private static Checkpoint parse(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalStateException("RBAC3_PROJECTION_CHECKPOINT_INVALID");
        }
        try {
            return new Checkpoint(parts[0], Long.parseLong(parts[1]), parts[2]);
        } catch (NumberFormatException invalid) {
            throw new IllegalStateException(
                    "RBAC3_PROJECTION_CHECKPOINT_INVALID", invalid);
        }
    }

    private static String part(String value) {
        if (value == null || !KEY_PART.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid RBAC3 projection key part");
        }
        return value;
    }

    private record Checkpoint(String eventId, long aggregateVersion, String state) {
    }
}
