package top.egon.cola.component.gateway.core.lifecycle;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic state machine shared by Engine listener, rule, provider and RPC slot
 * lifecycle coordination.
 */
public final class GatewayEngineLifecycle {

    private static final Map<GatewayEngineState, Set<GatewayEngineState>>
            TRANSITIONS = Map.of(
            GatewayEngineState.NEW,
            Set.of(GatewayEngineState.STARTING),
            GatewayEngineState.STARTING,
            Set.of(
                    GatewayEngineState.SYNCING_RULES,
                    GatewayEngineState.FAILED
            ),
            GatewayEngineState.SYNCING_RULES,
            Set.of(
                    GatewayEngineState.READY,
                    GatewayEngineState.FAILED
            ),
            GatewayEngineState.READY,
            Set.of(
                    GatewayEngineState.DEGRADED,
                    GatewayEngineState.DRAINING,
                    GatewayEngineState.FAILED
            ),
            GatewayEngineState.DEGRADED,
            Set.of(
                    GatewayEngineState.READY,
                    GatewayEngineState.DRAINING,
                    GatewayEngineState.FAILED
            ),
            GatewayEngineState.DRAINING,
            Set.of(
                    GatewayEngineState.STOPPED,
                    GatewayEngineState.FAILED
            ),
            GatewayEngineState.FAILED,
            Set.of(GatewayEngineState.STOPPED),
            GatewayEngineState.STOPPED,
            Set.of()
    );

    private final AtomicReference<GatewayEngineState> state =
            new AtomicReference<>(GatewayEngineState.NEW);

    public GatewayEngineState state() {
        return state.get();
    }

    public void transitionTo(GatewayEngineState target) {
        Objects.requireNonNull(target, "target");
        while (true) {
            GatewayEngineState current = state.get();
            if (current == target) {
                return;
            }
            if (!TRANSITIONS.get(current).contains(target)) {
                throw new IllegalStateException(
                        "illegal Gateway Engine state transition: "
                                + current + " -> " + target
                );
            }
            if (state.compareAndSet(current, target)) {
                return;
            }
        }
    }

    public boolean acceptingRequests() {
        GatewayEngineState current = state.get();
        return current == GatewayEngineState.READY
                || current == GatewayEngineState.DEGRADED;
    }
}
