package top.egon.cola.component.outbox.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.event.OutboxCommittedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxAfterCommitBufferTest {

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OutboxAfterCommitBuffer buffer = new OutboxAfterCommitBuffer(publisher);

    @BeforeEach
    void initializeSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldPublishOneOrderedDeduplicatedEventAfterCommit() {
        buffer.record("message-2");
        buffer.record("message-1");
        buffer.record("message-2");

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        synchronization().afterCommit();

        ArgumentCaptor<OutboxCommittedEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxCommittedEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().messageIds()).containsExactly("message-2", "message-1");
    }

    @Test
    void shouldNotPublishAfterRollback() {
        buffer.record("message-1");

        synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldSwallowPublisherFailureOnlyAfterCommit() {
        doThrow(new IllegalStateException("listener failed"))
                .when(publisher).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
        buffer.record("message-1");

        assertThatCode(() -> synchronization().afterCommit()).doesNotThrowAnyException();
    }

    private TransactionSynchronization synchronization() {
        return TransactionSynchronizationManager.getSynchronizations().getFirst();
    }
}
