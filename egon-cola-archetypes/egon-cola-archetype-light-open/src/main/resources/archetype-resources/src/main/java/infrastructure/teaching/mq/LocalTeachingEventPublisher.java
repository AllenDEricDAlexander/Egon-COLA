package ${package}.infrastructure.teaching.mq;

import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.vos.TeachingEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class LocalTeachingEventPublisher implements TeachingEventPublisher {
    private final List<TeachingEvent> publishedEvents = new CopyOnWriteArrayList<>();
    private final TransactionCompletionExecutor transactionCompletionExecutor;

    @Override
    public void publish(TeachingEvent event) {
        transactionCompletionExecutor.executeAfterCommit(() -> publishedEvents.add(event));
    }

    public List<TeachingEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
