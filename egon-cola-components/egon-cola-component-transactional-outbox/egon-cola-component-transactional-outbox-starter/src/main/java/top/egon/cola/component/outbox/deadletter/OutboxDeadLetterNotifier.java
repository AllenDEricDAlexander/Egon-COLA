package top.egon.cola.component.outbox.deadletter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.outbox.event.OutboxDeadLetterEvent;

import java.util.List;

public class OutboxDeadLetterNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDeadLetterNotifier.class);

    private final List<OutboxDeadLetterListener> listeners;

    public OutboxDeadLetterNotifier(List<OutboxDeadLetterListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    public void notifyDead(OutboxDeadLetterEvent event) {
        for (OutboxDeadLetterListener listener : listeners) {
            try {
                listener.onDead(event);
            } catch (RuntimeException exception) {
                LOGGER.warn("Transactional outbox DEAD listener failed");
            }
        }
    }
}
