package top.egon.cola.archetype.source.lightopen.infrastructure.user.mq;

import top.egon.cola.archetype.source.lightopen.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.lightopen.infrastructure.config.TransactionCompletionExecutor;
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
