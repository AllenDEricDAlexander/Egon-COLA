package top.egon.cola.archetype.source.light.infrastructure.teaching.mq;

import top.egon.cola.archetype.source.light.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
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
