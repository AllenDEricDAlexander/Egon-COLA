package top.egon.cola.component.outbox.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.store.OutboxStore;

public class OutboxCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxStore store;
    private final TransactionalOutboxProperties properties;

    public OutboxCleanupJob(OutboxStore store, TransactionalOutboxProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    public void runOnce() {
        if (!properties.getCleanup().isEnabled()) {
            return;
        }
        try {
            store.deleteSucceeded(
                    properties.getCleanup().getSuccessRetention(),
                    properties.getCleanup().getBatchSize()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Transactional outbox cleanup failed");
        }
    }
}
