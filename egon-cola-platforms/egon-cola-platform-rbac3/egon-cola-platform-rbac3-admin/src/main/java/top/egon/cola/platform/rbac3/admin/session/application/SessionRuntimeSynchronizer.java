package top.egon.cola.platform.rbac3.admin.session.application;

import java.time.Instant;

/**
 * Rebuilds one session runtime from authoritative PostgreSQL facts.
 */
@FunctionalInterface
public interface SessionRuntimeSynchronizer {

    void synchronize(String tenantId, String userId, String sessionId, Instant generatedAt);
}
