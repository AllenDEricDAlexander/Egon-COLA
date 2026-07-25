package top.egon.cola.component.gateway.core.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEngineLifecycleTest {

    @Test
    void followsTheNormalStartupAndDrainPath() {
        GatewayEngineLifecycle lifecycle = new GatewayEngineLifecycle();

        assertEquals(GatewayEngineState.NEW, lifecycle.state());
        assertFalse(lifecycle.acceptingRequests());

        lifecycle.transitionTo(GatewayEngineState.STARTING);
        lifecycle.transitionTo(GatewayEngineState.SYNCING_RULES);
        lifecycle.transitionTo(GatewayEngineState.READY);

        assertTrue(lifecycle.acceptingRequests());

        lifecycle.transitionTo(GatewayEngineState.DRAINING);
        assertFalse(lifecycle.acceptingRequests());
        lifecycle.transitionTo(GatewayEngineState.STOPPED);

        assertEquals(GatewayEngineState.STOPPED, lifecycle.state());
    }

    @Test
    void degradedNodeCanServeThenRecoverOrDrain() {
        GatewayEngineLifecycle recovering = readyLifecycle();
        recovering.transitionTo(GatewayEngineState.DEGRADED);

        assertTrue(recovering.acceptingRequests());

        recovering.transitionTo(GatewayEngineState.READY);
        assertEquals(GatewayEngineState.READY, recovering.state());

        GatewayEngineLifecycle draining = readyLifecycle();
        draining.transitionTo(GatewayEngineState.DEGRADED);
        draining.transitionTo(GatewayEngineState.DRAINING);
        assertFalse(draining.acceptingRequests());
    }

    @Test
    void startupSyncAndRuntimeFailuresAreTerminalUntilStopped() {
        for (GatewayEngineLifecycle lifecycle : List.of(
                startingLifecycle(),
                syncingLifecycle(),
                readyLifecycle()
        )) {
            lifecycle.transitionTo(GatewayEngineState.FAILED);

            assertFalse(lifecycle.acceptingRequests());
            assertThrows(
                    IllegalStateException.class,
                    () -> lifecycle.transitionTo(GatewayEngineState.READY)
            );
            lifecycle.transitionTo(GatewayEngineState.STOPPED);
            assertEquals(GatewayEngineState.STOPPED, lifecycle.state());
        }
    }

    @Test
    void illegalJumpsAreRejectedAndSameStateIsIdempotent() {
        GatewayEngineLifecycle lifecycle = new GatewayEngineLifecycle();

        lifecycle.transitionTo(GatewayEngineState.NEW);
        assertEquals(GatewayEngineState.NEW, lifecycle.state());
        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.transitionTo(GatewayEngineState.READY)
        );

        lifecycle.transitionTo(GatewayEngineState.STARTING);
        lifecycle.transitionTo(GatewayEngineState.STARTING);
        assertEquals(GatewayEngineState.STARTING, lifecycle.state());
    }

    private GatewayEngineLifecycle startingLifecycle() {
        GatewayEngineLifecycle lifecycle = new GatewayEngineLifecycle();
        lifecycle.transitionTo(GatewayEngineState.STARTING);
        return lifecycle;
    }

    private GatewayEngineLifecycle syncingLifecycle() {
        GatewayEngineLifecycle lifecycle = startingLifecycle();
        lifecycle.transitionTo(GatewayEngineState.SYNCING_RULES);
        return lifecycle;
    }

    private GatewayEngineLifecycle readyLifecycle() {
        GatewayEngineLifecycle lifecycle = syncingLifecycle();
        lifecycle.transitionTo(GatewayEngineState.READY);
        return lifecycle;
    }
}
