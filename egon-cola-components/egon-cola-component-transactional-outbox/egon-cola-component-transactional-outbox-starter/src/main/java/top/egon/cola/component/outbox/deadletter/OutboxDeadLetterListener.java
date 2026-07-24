package top.egon.cola.component.outbox.deadletter;

import top.egon.cola.component.outbox.event.OutboxDeadLetterEvent;

@FunctionalInterface
public interface OutboxDeadLetterListener {

    void onDead(OutboxDeadLetterEvent event);
}
