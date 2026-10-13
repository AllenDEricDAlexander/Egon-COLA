package top.egon.cola.component.outbox.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionMismatchException;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionRequiredException;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionSynchronizationException;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
public class OutboxTransactionGuard {

    @Qualifier("dataSource")
    private final DataSource selectedDataSource;

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
        Object resource = TransactionSynchronizationManager.getResource(selectedDataSource);
        if (!(resource instanceof ConnectionHolder holder) || holder.getConnectionHandle() == null) {
            throw new OutboxTransactionMismatchException(
                    "The active transaction does not use the configured outbox DataSource"
            );
        }
        Connection connection = holder.getConnection();
        if (connection == null || !DataSourceUtils.isConnectionTransactional(connection, selectedDataSource)) {
            throw new OutboxTransactionMismatchException(
                    "The active transaction does not use the configured outbox DataSource"
            );
        }
    }
}
