package top.egon.cola.platform.rbac3.admin.activation.repository;

import java.time.Instant;

/**
 * Marks user authorization as requiring a fresh active-role selection.
 */
@FunctionalInterface
public interface ReselectionRepository {

    void requireReselection(
            String tenantId,
            String userId,
            long expectedAuthVersion,
            Instant now,
            String actorId);
}
