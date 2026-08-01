package top.egon.cola.platform.rbac3.admin.integration.runtime;

import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.integration.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/** Produces independent health facts for RBAC3 persistence and projection subsystems. */
@Repository
public class Rbac3OperationalRuntimeStatusService {

    private static final List<AuthorizationMutationEntity.Status> PENDING_MUTATIONS = List.of(
            AuthorizationMutationEntity.Status.PREPARING,
            AuthorizationMutationEntity.Status.COMMITTED,
            AuthorizationMutationEntity.Status.PROJECTED,
            AuthorizationMutationEntity.Status.RECOVERY_REQUIRED);

    private final EntityManager entityManager;
    private final Flyway rbac3Flyway;
    private final Flyway outboxFlyway;
    private final RedissonClient redisson;
    private final Clock clock;

    public Rbac3OperationalRuntimeStatusService(
            EntityManager entityManager,
            @Qualifier(Rbac3FlywayConfiguration.RBAC3_FLYWAY) Flyway rbac3Flyway,
            @Qualifier(Rbac3FlywayConfiguration.OUTBOX_FLYWAY) Flyway outboxFlyway,
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            Clock clock) {
        this.entityManager = entityManager;
        this.rbac3Flyway = rbac3Flyway;
        this.outboxFlyway = outboxFlyway;
        this.redisson = redisson;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OperationalStatus status() {
        MutationFacts mutations = mutationFacts();
        return new OperationalStatus(
                new ControlPlaneRuntimeStatusPort.FlywayStatus(
                        flywayState(rbac3Flyway), flywayState(outboxFlyway)),
                redisStatus(mutations.projectionLag()),
                new ControlPlaneRuntimeStatusPort.FenceMutationStatus(
                        mutationState(mutations), mutations.pendingCount(),
                        mutations.recoveryRequiredCount(), mutations.oldestAgeSeconds()),
                outboxStatus());
    }

    private String flywayState(Flyway flyway) {
        try {
            return flyway.info().pending().length == 0 && flyway.info().current() != null
                    ? "UP_TO_DATE" : "PENDING";
        } catch (RuntimeException unavailable) {
            return "UNAVAILABLE";
        }
    }

    private ControlPlaneRuntimeStatusPort.RedisProjectionStatus redisStatus(long lag) {
        try {
            redisson.getKeys().count();
            return new ControlPlaneRuntimeStatusPort.RedisProjectionStatus(
                    lag == 0 ? "HEALTHY" : "LAGGING", lag);
        } catch (RuntimeException unavailable) {
            return new ControlPlaneRuntimeStatusPort.RedisProjectionStatus("UNAVAILABLE", lag);
        }
    }

    private MutationFacts mutationFacts() {
        try {
            long pending = entityManager.createQuery("""
                            select count(m) from AuthorizationMutationEntity m
                             where m.status in :statuses
                            """, Long.class)
                    .setParameter("statuses", PENDING_MUTATIONS)
                    .getSingleResult();
            long recovery = entityManager.createQuery("""
                            select count(m) from AuthorizationMutationEntity m
                             where m.status = :status
                            """, Long.class)
                    .setParameter("status", AuthorizationMutationEntity.Status.RECOVERY_REQUIRED)
                    .getSingleResult();
            Instant oldest = entityManager.createQuery("""
                            select min(m.updatedAt) from AuthorizationMutationEntity m
                             where m.status in :statuses
                            """, Instant.class)
                    .setParameter("statuses", PENDING_MUTATIONS)
                    .getSingleResult();
            long projectionLag = entityManager.createQuery("""
                            select count(m) from AuthorizationMutationEntity m
                             where m.status in :statuses
                            """, Long.class)
                    .setParameter("statuses", List.of(
                            AuthorizationMutationEntity.Status.COMMITTED,
                            AuthorizationMutationEntity.Status.RECOVERY_REQUIRED))
                    .getSingleResult();
            return new MutationFacts(pending, recovery, ageSeconds(oldest), projectionLag, true);
        } catch (RuntimeException unavailable) {
            return new MutationFacts(0L, 0L, 0L, 0L, false);
        }
    }

    private String mutationState(MutationFacts facts) {
        if (!facts.available()) {
            return "UNAVAILABLE";
        }
        if (facts.recoveryRequiredCount() > 0) {
            return "DEGRADED";
        }
        return facts.pendingCount() == 0 ? "HEALTHY" : "ACTIVE";
    }

    private ControlPlaneRuntimeStatusPort.OutboxStatus outboxStatus() {
        try {
            long pending = ((Number) entityManager.createNativeQuery("""
                            select count(*) from egon_cola_outbox_message
                             where status in ('PENDING', 'PROCESSING', 'RETRY_WAIT')
                            """)
                    .getSingleResult()).longValue();
            long dead = ((Number) entityManager.createNativeQuery("""
                            select count(*) from egon_cola_outbox_message where status = 'DEAD'
                            """)
                    .getSingleResult()).longValue();
            Object oldestValue = entityManager.createNativeQuery("""
                            select min(created_at) from egon_cola_outbox_message
                             where status in ('PENDING', 'PROCESSING', 'RETRY_WAIT')
                            """)
                    .getSingleResult();
            Instant oldest = oldestValue instanceof Instant instant
                    ? instant
                    : oldestValue instanceof OffsetDateTime value ? value.toInstant() : null;
            String state = dead > 0 ? "DEGRADED" : pending > 0 ? "LAGGING" : "HEALTHY";
            return new ControlPlaneRuntimeStatusPort.OutboxStatus(
                    state, pending, ageSeconds(oldest));
        } catch (RuntimeException unavailable) {
            return new ControlPlaneRuntimeStatusPort.OutboxStatus("UNAVAILABLE", 0L, 0L);
        }
    }

    private long ageSeconds(Instant value) {
        if (value == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(value, clock.instant()).toSeconds());
    }

    public record OperationalStatus(
            ControlPlaneRuntimeStatusPort.FlywayStatus flyway,
            ControlPlaneRuntimeStatusPort.RedisProjectionStatus redisProjection,
            ControlPlaneRuntimeStatusPort.FenceMutationStatus fence,
            ControlPlaneRuntimeStatusPort.OutboxStatus outbox) {
    }

    private record MutationFacts(
            long pendingCount,
            long recoveryRequiredCount,
            long oldestAgeSeconds,
            long projectionLag,
            boolean available) {
    }
}
