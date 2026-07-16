package top.egon.cola.component.bytecode.runtime.observation;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.api.observation.ObservationResult;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.EgonObservationBridge;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationMetadata;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationRuntimeTest {

    @Test
    void noDispatcherAndDisabledRuntimeAreNoOps() {
        assertTrue(EgonObservationBridge.enter(Owner.class, 91_001L).noOp());
        ObservationRuntime runtime = new ObservationRuntime(
                false, 1.0, List.of(event -> { throw new AssertionError(); }),
                new BoundedFailureStore(4));
        assertNull(runtime.enter(Owner.class, 91_001L));
    }

    @Test
    void successAndErrorPublishOnceWithoutRetainingThrowable() {
        long successId = register(91_002L, "success", 1_000L);
        long errorId = register(91_003L, "error", 1_000L);
        List<ObservationEvent> events = new ArrayList<>();
        ObservationRuntime runtime = new ObservationRuntime(
                true, 1.0, List.of(events::add), new BoundedFailureStore(4));

        ObservationState success = runtime.enter(Owner.class, successId);
        runtime.success(success);
        runtime.exit(success);
        runtime.exit(success);

        IllegalStateException original = new IllegalStateException("secret-message");
        ObservationState error = runtime.enter(Owner.class, errorId);
        assertSame(original, runtime.error(error, original));
        runtime.exit(error);

        assertEquals(2, events.size());
        assertEquals(ObservationResult.SUCCESS, events.get(0).result());
        assertEquals(ObservationResult.ERROR, events.get(1).result());
        assertEquals("IllegalStateException", events.get(1).exceptionGroup());
        assertFalseContains(events.get(1).toString(), "secret-message");
    }

    @Test
    void sinkFailuresAreBoundedAndSinkRecursionIsSuppressed() {
        long methodId = register(91_004L, "recursive", 1_000L);
        BoundedFailureStore failures = new BoundedFailureStore(2);
        AtomicBoolean recursionSuppressed = new AtomicBoolean();
        ObservationRuntime[] holder = new ObservationRuntime[1];
        holder[0] = new ObservationRuntime(true, 1.0, List.of(event -> {
            recursionSuppressed.set(holder[0].enter(Owner.class, methodId) == null);
            throw new IllegalArgumentException("sink failure");
        }), failures);

        ObservationState state = holder[0].enter(Owner.class, methodId);
        holder[0].success(state);
        holder[0].exit(state);

        assertTrue(recursionSuppressed.get());
        assertEquals(1, failures.failures().size());
        assertEquals(IllegalArgumentException.class.getName(),
                failures.failures().getFirst().exceptionType());
    }

    private long register(long methodId, String methodName, long slowThresholdNanos) {
        ClassLoader loader = Owner.class.getClassLoader();
        DispatcherRegistry.registerMethod(loader, new MethodMetadata(
                methodId, Owner.class.getName().replace('.', '/'), methodName, "()V",
                java.lang.reflect.Modifier.PUBLIC, false,
                Set.of(BridgeCapability.OBSERVATION)));
        DispatcherRegistry.registerObservation(loader, new ObservationMetadata(
                methodId, "APPLICATION", Map.of("channel", "test"), slowThresholdNanos));
        return methodId;
    }

    private void assertFalseContains(String value, String forbidden) {
        assertTrue(!value.contains(forbidden));
    }

    private static final class Owner {
    }
}
