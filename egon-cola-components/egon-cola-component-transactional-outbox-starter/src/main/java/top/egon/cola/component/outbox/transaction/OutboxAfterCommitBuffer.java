package top.egon.cola.component.outbox.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.event.OutboxCommittedEvent;
import top.egon.cola.component.outbox.observability.OutboxMetrics;

import java.util.LinkedHashSet;

public class OutboxAfterCommitBuffer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxAfterCommitBuffer.class);

    private final ApplicationEventPublisher publisher;
    private final OutboxMetrics metrics;

    public OutboxAfterCommitBuffer(ApplicationEventPublisher publisher, OutboxMetrics metrics) {
        this.publisher = publisher;
        this.metrics = metrics;
    }

    public void record(String messageId) {
        OutboxSynchronization synchronization = TransactionSynchronizationManager
                .getSynchronizations()
                .stream()
                .filter(OutboxSynchronization.class::isInstance)
                .map(OutboxSynchronization.class::cast)
                .findFirst()
                .orElseGet(this::registerSynchronization);
        synchronization.messageIds.add(messageId);
    }

    private OutboxSynchronization registerSynchronization() {
        OutboxSynchronization synchronization = new OutboxSynchronization();
        TransactionSynchronizationManager.registerSynchronization(synchronization);
        return synchronization;
    }

    private final class OutboxSynchronization implements TransactionSynchronization {

        private final LinkedHashSet<String> messageIds = new LinkedHashSet<>();

        @Override
        public void afterCommit() {
            try {
                publisher.publishEvent(new OutboxCommittedEvent(messageIds.stream().toList()));
            } catch (RuntimeException exception) {
                metrics.wakeupRejected();
                LOGGER.warn(
                        "Transactional outbox post-commit wake event failed for {} message(s)",
                        messageIds.size()
                );
            }
        }

        @Override
        public void afterCompletion(int status) {
            messageIds.clear();
        }
    }
}
