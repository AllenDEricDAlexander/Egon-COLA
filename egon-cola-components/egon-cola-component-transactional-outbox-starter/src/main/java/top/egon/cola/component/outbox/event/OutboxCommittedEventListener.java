package top.egon.cola.component.outbox.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import top.egon.cola.component.outbox.dispatch.OutboxDispatcher;

public class OutboxCommittedEventListener
        implements ApplicationListener<PayloadApplicationEvent<OutboxCommittedEvent>> {

    private final OutboxDispatcher dispatcher;

    public OutboxCommittedEventListener(OutboxDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void onApplicationEvent(PayloadApplicationEvent<OutboxCommittedEvent> event) {
        dispatcher.submitMessageIds(event.getPayload().messageIds());
    }
}
