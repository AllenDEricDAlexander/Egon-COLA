package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.plan.GuardPlanChangedEvent;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class CompositeGuardEventPublisher implements GuardEventPublisher {

    private final List<GuardEventListener> listeners;

    public CompositeGuardEventPublisher(List<GuardEventListener> listeners) {
        this.listeners = List.copyOf(Objects.requireNonNull(listeners, "listeners"));
    }

    @Override
    public void publish(GuardEvent event) {
        notifyListeners(listener -> listener.onEvent(event));
    }

    @Override
    public void publishStage(GuardStageEvent event) {
        notifyListeners(listener -> listener.onStage(event));
    }

    @Override
    public void publishPlanChanged(GuardPlanChangedEvent event) {
        notifyListeners(listener -> listener.onPlanChanged(event));
    }

    private void notifyListeners(Consumer<GuardEventListener> notification) {
        for (GuardEventListener listener : listeners) {
            try {
                notification.accept(listener);
            } catch (RuntimeException ignored) {
                // Observability is fail-open and cannot change an established guard outcome.
            }
        }
    }
}
