package top.egon.cola.component.bytecode.runtime.observation;

import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.api.observation.ObservationEventSink;
import top.egon.cola.component.bytecode.api.observation.ObservationResult;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationMetadata;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class ObservationRuntime {

    private final boolean enabled;
    private final double samplingRate;
    private final List<ObservationEventSink> sinks;
    private final BoundedFailureStore failureStore;
    private final ObservationReentrancyGuard reentrancyGuard =
            new ObservationReentrancyGuard();

    public ObservationRuntime(
            boolean enabled,
            double samplingRate,
            List<? extends ObservationEventSink> sinks,
            BoundedFailureStore failureStore
    ) {
        if (samplingRate < 0.0 || samplingRate > 1.0) {
            throw new IllegalArgumentException("samplingRate must be between 0 and 1");
        }
        this.enabled = enabled;
        this.samplingRate = samplingRate;
        this.sinks = List.copyOf(sinks);
        this.failureStore = Objects.requireNonNull(failureStore, "failureStore");
    }

    public boolean enabled() {
        return enabled;
    }

    public ObservationState enter(Class<?> declaringClass, long methodId) {
        if (!enabled || reentrancyGuard.active() || !sampled()) {
            return null;
        }
        ClassLoader loader = declaringClass == null ? null : declaringClass.getClassLoader();
        MethodMetadata method = DispatcherRegistry.method(loader, methodId).orElse(null);
        ObservationMetadata observation = DispatcherRegistry
                .observation(loader, methodId).orElse(null);
        if (method == null || observation == null
                || !method.features().contains(BridgeCapability.OBSERVATION)) {
            return null;
        }
        return new ObservationState(method, observation, System.nanoTime());
    }

    public void success(ObservationState state) {
        if (state != null) {
            state.result = ObservationResult.SUCCESS;
            state.exceptionGroup = "NONE";
        }
    }

    public <T extends Throwable> T error(ObservationState state, T throwable) {
        if (state != null) {
            state.result = ObservationResult.ERROR;
            state.exceptionGroup = throwable == null
                    ? "Throwable" : throwable.getClass().getSimpleName();
        }
        return throwable;
    }

    public void exit(ObservationState state) {
        if (state == null || state.exited) {
            return;
        }
        state.exited = true;
        if (state.result == null) {
            return;
        }
        long duration = Math.max(0L, System.nanoTime() - state.startedNanos);
        ObservationEvent event = new ObservationEvent(
                state.method.methodId(),
                state.method.owner().replace('/', '.'),
                state.method.methodName(),
                state.method.methodDescriptor(),
                state.observation.layer(),
                duration,
                state.result,
                state.exceptionGroup,
                "",
                Thread.currentThread().isVirtual(),
                state.observation.staticTags(),
                state.observation.slowThresholdNanos()
        );
        reentrancyGuard.run(() -> publish(event));
    }

    public BoundedFailureStore failureStore() {
        return failureStore;
    }

    private boolean sampled() {
        if (samplingRate >= 1.0) {
            return true;
        }
        if (samplingRate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < samplingRate;
    }

    private void publish(ObservationEvent event) {
        for (ObservationEventSink sink : sinks) {
            try {
                sink.publish(event);
            } catch (Throwable failure) {
                failureStore.record(sink.getClass().getName(), failure);
            }
        }
    }
}
