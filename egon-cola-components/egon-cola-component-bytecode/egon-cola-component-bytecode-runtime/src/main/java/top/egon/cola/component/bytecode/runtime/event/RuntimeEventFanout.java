package top.egon.cola.component.bytecode.runtime.event;

import top.egon.cola.component.bytecode.api.executor.ExecutorEvent;
import top.egon.cola.component.bytecode.api.executor.ExecutorEventSink;

import java.util.List;
import java.util.Objects;

public final class RuntimeEventFanout {

    private final List<ExecutorEventSink> sinks;
    private final BoundedFailureStore failureStore;

    public RuntimeEventFanout(
            List<? extends ExecutorEventSink> sinks,
            BoundedFailureStore failureStore
    ) {
        this.sinks = List.copyOf(sinks);
        this.failureStore = Objects.requireNonNull(failureStore, "failureStore");
    }

    public void publish(ExecutorEvent event) {
        for (ExecutorEventSink sink : sinks) {
            try {
                sink.publish(event);
            } catch (Throwable failure) {
                failureStore.record(sink.getClass().getName(), failure);
            }
        }
    }

    public BoundedFailureStore failureStore() {
        return failureStore;
    }
}
