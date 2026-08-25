package ${package}.infrastructure.teaching.mq;

import ${package}.domain.teaching.event.TeachingEventPublisher;
import ${package}.domain.teaching.vos.TeachingEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@Slf4j
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
