package top.egon.cola.platform.rbac3.admin.worker;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.assignment.domain.UserRoleAssignmentEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns due assignment rows until their state change and Outbox enqueue commit.
 */
@Repository
public class PostgresqlAssignmentLifecycleStore
        implements AssignmentLifecycleWorker.LifecycleStore {

    private static final String ACTOR = "rbac3-assignment-lifecycle-worker";

    private final EntityManager entityManager;

    public PostgresqlAssignmentLifecycleStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int processDue(
            Instant now,
            int batchSize,
            AssignmentLifecycleWorker.ChangePublisher publisher) {
        List<DueAssignment> due = new ArrayList<>(batchSize);
        due.addAll(lockDue("PENDING", "valid_from <= :now", "ACTIVATED",
                now, batchSize));
        if (due.size() < batchSize) {
            due.addAll(lockDue("ACTIVE", "valid_to is not null and valid_to <= :now",
                    "EXPIRED", now, batchSize - due.size()));
        }
        if (due.size() < batchSize) {
            due.addAll(lockDue("SUSPENDED", "valid_to is not null and valid_to <= :now",
                    "EXPIRED", now, batchSize - due.size()));
        }
        for (DueAssignment candidate : due) {
            UserRoleAssignmentEntity assignment = entityManager.find(
                    UserRoleAssignmentEntity.class, candidate.assignmentId(),
                    LockModeType.PESSIMISTIC_WRITE);
            UserEntity user = entityManager.find(
                    UserEntity.class, assignment.getUserId(),
                    LockModeType.PESSIMISTIC_WRITE);
            long authVersion = user.advanceAuthorizationVersion(
                    user.getAuthVersion(), ACTOR, now);
            if ("ACTIVATED".equals(candidate.changeType())) {
                assignment.activate(ACTOR, now);
            } else {
                assignment.expire(ACTOR, now);
            }
            publisher.publish(new AssignmentLifecycleWorker.LifecycleChange(
                    assignment.getTenantId().toString(),
                    assignment.getId().toString(),
                    assignment.getUserId().toString(),
                    candidate.changeType(),
                    authVersion));
        }
        entityManager.flush();
        return due.size();
    }

    @SuppressWarnings("unchecked")
    private List<DueAssignment> lockDue(
            String status,
            String timePredicate,
            String changeType,
            Instant now,
            int limit) {
        if (limit == 0) {
            return List.of();
        }
        String sql = """
                select id
                  from rbac3_user_role_assignment
                 where status = :status and %s
                 order by coalesce(valid_to, valid_from), id
                 for update skip locked
                 limit :batchSize
                """.formatted(timePredicate);
        List<Number> ids = entityManager.createNativeQuery(sql)
                .setParameter("status", status)
                .setParameter("now", now)
                .setParameter("batchSize", limit)
                .getResultList();
        return ids.stream()
                .map(Number::longValue)
                .map(id -> new DueAssignment(id, changeType))
                .toList();
    }

    private record DueAssignment(Long assignmentId, String changeType) {
    }
}
