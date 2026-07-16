package top.egon.cola.component.bytecode.api.executor;

@FunctionalInterface
public interface ExecutorEventSink {

    void publish(ExecutorEvent event);
}
