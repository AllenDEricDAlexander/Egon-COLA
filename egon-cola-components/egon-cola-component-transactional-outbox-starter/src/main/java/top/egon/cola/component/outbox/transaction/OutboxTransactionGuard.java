package top.egon.cola.component.outbox.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.exception.OutboxTransactionMismatchException;
import top.egon.cola.component.outbox.exception.OutboxTransactionRequiredException;
import top.egon.cola.component.outbox.exception.OutboxTransactionSynchronizationException;

import javax.sql.DataSource;

public class OutboxTransactionGuard {

    private final DataSource selectedDataSource;

    public OutboxTransactionGuard(DataSource selectedDataSource) {
        this.selectedDataSource = selectedDataSource;
    }

    public void requireSelectedTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new OutboxTransactionRequiredException(
                    "Transactional outbox requires an active Spring transaction"
            );
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new OutboxTransactionSynchronizationException(
                    "Transactional outbox requires active transaction synchronization"
            );
        }
        if (!TransactionSynchronizationManager.hasResource(selectedDataSource)) {
            throw new OutboxTransactionMismatchException(
                    "The active transaction does not use the configured outbox DataSource"
            );
        }
    }
}
