package top.egon.cola.component.outbox.event;

import java.util.List;

public record OutboxCommittedEvent(List<String> messageIds) {

    public OutboxCommittedEvent {
        messageIds = List.copyOf(messageIds);
    }
}
