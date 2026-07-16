package top.egon.cola.component.methodextension.event;

@FunctionalInterface
public interface MethodExtensionEventPublisher {

    void publish(MethodExtensionEvent event);
}
