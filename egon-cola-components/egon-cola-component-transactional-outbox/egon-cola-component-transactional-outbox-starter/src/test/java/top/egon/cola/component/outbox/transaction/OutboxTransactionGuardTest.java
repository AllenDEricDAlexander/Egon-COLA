package top.egon.cola.component.outbox.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.exception.OutboxTransactionMismatchException;
import top.egon.cola.component.outbox.exception.OutboxTransactionRequiredException;
import top.egon.cola.component.outbox.exception.OutboxTransactionSynchronizationException;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OutboxTransactionGuardTest {

    private final DataSource dataSource = mock(DataSource.class);

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.hasResource(dataSource)) {
            TransactionSynchronizationManager.unbindResource(dataSource);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldRequireAnActualTransaction() {
        OutboxTransactionGuard guard = new OutboxTransactionGuard(dataSource);

        assertThatThrownBy(guard::requireSelectedTransaction)
                .isInstanceOf(OutboxTransactionRequiredException.class);
    }

    @Test
    void shouldRequireTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        OutboxTransactionGuard guard = new OutboxTransactionGuard(dataSource);

        assertThatThrownBy(guard::requireSelectedTransaction)
                .isInstanceOf(OutboxTransactionSynchronizationException.class);
    }

    @Test
    void shouldRequireTheConfiguredDataSourceInTheActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        OutboxTransactionGuard guard = new OutboxTransactionGuard(dataSource);

        assertThatThrownBy(guard::requireSelectedTransaction)
                .isInstanceOf(OutboxTransactionMismatchException.class);
    }

    @Test
    void shouldAcceptTheConfiguredDataSourceResource() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.bindResource(
                dataSource,
                new ConnectionHolder(mock(Connection.class))
        );
        OutboxTransactionGuard guard = new OutboxTransactionGuard(dataSource);

        assertThatCode(guard::requireSelectedTransaction).doesNotThrowAnyException();
    }
}
