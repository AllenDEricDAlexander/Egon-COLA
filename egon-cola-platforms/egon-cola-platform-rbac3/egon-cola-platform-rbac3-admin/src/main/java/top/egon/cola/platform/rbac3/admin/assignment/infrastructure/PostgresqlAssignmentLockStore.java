package top.egon.cola.platform.rbac3.admin.assignment.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Repository
public class PostgresqlAssignmentLockStore implements AssignmentFacade.AssignmentLock {

    private final EntityManager entityManager;

    public PostgresqlAssignmentLockStore(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    @Transactional
    public Object withLock(AssignmentFacade.LockExecution scope) {
        String canonical = canonicalKey(
                scope.tenantId(), scope.activationRootRoleId(),
                scope.scopeType(), scope.scopeId());
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
                .setParameter("lockId", advisoryLockId(canonical))
                .getSingleResult();
        return scope.action().get();
    }

    public static String canonicalKey(
            String tenantId,
            String activationRootRoleId,
            String scopeType,
            String scopeId
    ) {
        return required(tenantId, "tenantId") + '|'
                + required(activationRootRoleId, "activationRootRoleId") + '|'
                + required(scopeType, "scopeType") + '|'
                + required(scopeId, "scopeId");
    }

    public static long advisoryLockId(String canonicalKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    nonBlank(canonicalKey, "canonicalKey")
                            .getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
            return value == 0L ? 1L : value;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(name + " is not a safe lock segment");
        }
        return value.trim();
    }

    private static String nonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
