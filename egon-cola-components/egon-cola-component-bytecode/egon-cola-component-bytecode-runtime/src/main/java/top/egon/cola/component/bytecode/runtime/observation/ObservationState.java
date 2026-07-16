package top.egon.cola.component.bytecode.runtime.observation;

import top.egon.cola.component.bytecode.api.observation.ObservationResult;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationMetadata;

public final class ObservationState {

    final MethodMetadata method;
    final ObservationMetadata observation;
    final long startedNanos;
    ObservationResult result;
    String exceptionGroup = "NONE";
    boolean exited;

    ObservationState(
            MethodMetadata method,
            ObservationMetadata observation,
            long startedNanos
    ) {
        this.method = method;
        this.observation = observation;
        this.startedNanos = startedNanos;
    }
}
