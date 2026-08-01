package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationFenceService;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MutationFenceRollbackIT {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void rollbackCreatesNeitherFenceNorProjection() {
        AtomicBoolean fenced = new AtomicBoolean();
        AtomicBoolean projected = new AtomicBoolean();
        AuthorizationMutationCoordinator coordinator = coordinator(
                fenced, projected, true);

        assertThatThrownBy(() -> coordinator.execute(
                scope(), "20001", versions(), () -> {
                    throw new IllegalStateException("database rejected");
                })).isInstanceOf(IllegalStateException.class);
        assertThat(fenced).isFalse();
        assertThat(projected).isFalse();
    }

    @Test
    void projectionFailureKeepsFenceAndReturnsPendingMutation() {
        AtomicBoolean fenced = new AtomicBoolean();
        AtomicBoolean projected = new AtomicBoolean();
        AuthorizationMutationCoordinator coordinator = coordinator(
                fenced, projected, false);

        var result = coordinator.execute(scope(), "20001", versions(), () -> "saved");

        assertThat(result.completed()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("AUTH_PROPAGATION_PENDING");
        assertThat(fenced).isTrue();
        assertThat(projected).isTrue();
    }

    private AuthorizationMutationCoordinator coordinator(
            AtomicBoolean fenced,
            AtomicBoolean projected,
            boolean projectionSucceeds
    ) {
        List<String> states = new ArrayList<>();
        var store = new AuthorizationMutationCoordinator.MutationStore() {
            public void prepare(AuthorizationMutationCoordinator.MutationRecord record) {
                states.add("PREPARING");
            }

            public void transition(
                    String mutationId,
                    AuthorizationMutationCoordinator.MutationStatus status,
                    String errorCode,
                    Instant now
            ) {
                states.add(status.name());
            }
        };
        AuthorizationFenceService fence = new AuthorizationFenceService(
                new AuthorizationFenceService.FenceStore() {
                    public void put(AuthorizationFenceService.Fence fence) {
                        fenced.set(true);
                    }

                    public void remove(String tenantId, String scopeType, String scopeId) {
                        fenced.set(false);
                    }
                }, Clock.fixed(NOW, ZoneOffset.UTC));
        return new AuthorizationMutationCoordinator(
                store,
                fence,
                mutation -> {
                    projected.set(true);
                    if (!projectionSucceeds) {
                        throw new IllegalStateException("redis unavailable");
                    }
                },
                supplier -> supplier.get(),
                () -> "70001",
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthorizationMutationCoordinator.MutationScope scope() {
        return new AuthorizationMutationCoordinator.MutationScope(
                "10001", "USER", "20001", "command-1", "operator");
    }

    private AuthorizationMutationCoordinator.ExpectedVersions versions() {
        return new AuthorizationMutationCoordinator.ExpectedVersions(
                null, null, 3L, 4L, 7L, 7L);
    }
}
