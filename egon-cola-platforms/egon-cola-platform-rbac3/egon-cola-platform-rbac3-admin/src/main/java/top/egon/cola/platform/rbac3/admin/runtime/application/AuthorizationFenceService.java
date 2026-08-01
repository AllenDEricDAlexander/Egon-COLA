package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Maintains fail-closed authorization fences during runtime propagation.
 */
public final class AuthorizationFenceService {

    private final FenceStore store;
    private final Clock clock;

    public AuthorizationFenceService(FenceStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void create(String tenantId, String scopeType, String scopeId, String mutationId) {
        store.put(new Fence(
                tenantId, scopeType, scopeId, mutationId, clock.instant()));
    }

    public void release(String tenantId, String scopeType, String scopeId) {
        store.remove(tenantId, scopeType, scopeId);
    }

    public interface FenceStore {
        void put(Fence fence);

        void remove(String tenantId, String scopeType, String scopeId);
    }

    public record Fence(
            String tenantId,
            String scopeType,
            String scopeId,
            String mutationId,
            Instant createdAt
    ) {
    }
}
