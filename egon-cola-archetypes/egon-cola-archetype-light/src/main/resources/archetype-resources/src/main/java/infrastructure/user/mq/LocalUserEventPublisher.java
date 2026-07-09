package ${package}.infrastructure.user.mq;

import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.UserEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalUserEventPublisher implements UserEventPublisher {
    private final List<UserEvent> publishedEvents = new CopyOnWriteArrayList<>();
    private final TransactionCompletionExecutor transactionCompletionExecutor;

    public LocalUserEventPublisher(TransactionCompletionExecutor transactionCompletionExecutor) {
        this.transactionCompletionExecutor = transactionCompletionExecutor;
    }

    @Override
    public void publish(UserEvent event) {
        transactionCompletionExecutor.executeAfterCommit(() -> publishedEvents.add(event));
    }

    public List<UserEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
