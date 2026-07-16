package top.egon.cola.component.bytecode.api.observation;

@FunctionalInterface
public interface ObservationEventSink {

    void publish(ObservationEvent event);
}
