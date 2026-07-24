package top.egon.cola.component.outbox.cleanup;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.store.OutboxStore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxCleanupJobTest {

    @Test
    void shouldDeleteOnlyWhenCleanupIsEnabled() {
        OutboxStore store = mock(OutboxStore.class);
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getCleanup().setEnabled(true);
        OutboxCleanupJob cleanupJob = new OutboxCleanupJob(store, properties);

        cleanupJob.runOnce();

        verify(store).deleteSucceeded(
                properties.getCleanup().getSuccessRetention(),
                properties.getCleanup().getBatchSize()
        );
    }

    @Test
    void shouldDoNothingWhenCleanupIsDisabled() {
        OutboxStore store = mock(OutboxStore.class);
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        OutboxCleanupJob cleanupJob = new OutboxCleanupJob(store, properties);

        cleanupJob.runOnce();

        verifyNoInteractions(store);
    }
}
