package ${package}.infrastructure.user.mq;

import ${package}.domain.user.event.UserEventPublisher;
import ${package}.domain.user.vos.UserEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@Slf4j
public class LocalUserEventPublisher implements UserEventPublisher {
    private final List<UserEvent> publishedEvents = new CopyOnWriteArrayList<>();
    private final TransactionCompletionExecutor transactionCompletionExecutor;

    @Override
    public void publish(UserEvent event) {
        transactionCompletionExecutor.executeAfterCommit(() -> publishedEvents.add(event));
    }

    public List<UserEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
