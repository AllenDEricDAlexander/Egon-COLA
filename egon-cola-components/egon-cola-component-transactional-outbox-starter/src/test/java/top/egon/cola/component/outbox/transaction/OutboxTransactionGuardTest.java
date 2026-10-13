package top.egon.cola.component.outbox.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionMismatchException;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionRequiredException;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionSynchronizationException;

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
    @Test
    void rejectsNonConnectionHolder() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.bindResource(dataSource, new Object());
        assertThatThrownBy(new OutboxTransactionGuard(dataSource)::requireSelectedTransaction)
                .isInstanceOf(OutboxTransactionMismatchException.class);
    }

    @Test
    void rejectsEmptyConnectionHandle() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.bindResource(dataSource,
                mock(ConnectionHolder.class));
        assertThatThrownBy(new OutboxTransactionGuard(dataSource)::requireSelectedTransaction)
                .isInstanceOf(OutboxTransactionMismatchException.class);
    }

    @Test
    void rejectsHolderWhoseHandleCannotProvideAConnection() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        var handle = mock(org.springframework.jdbc.datasource.ConnectionHandle.class);
        TransactionSynchronizationManager.bindResource(dataSource, new ConnectionHolder(handle));
        assertThatThrownBy(new OutboxTransactionGuard(dataSource)::requireSelectedTransaction)
                .isInstanceOf(OutboxTransactionMismatchException.class);
    }

    @Test
    void acceptsJpaLikeHolderWithoutJdbcSynchronizationFlag() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        ConnectionHolder holder = new ConnectionHolder(mock(Connection.class));
        org.assertj.core.api.Assertions.assertThat(holder.isSynchronizedWithTransaction()).isFalse();
        TransactionSynchronizationManager.bindResource(dataSource, holder);
        assertThatCode(new OutboxTransactionGuard(dataSource)::requireSelectedTransaction).doesNotThrowAnyException();
    }

    @Test
    void acceptsJdbcSynchronizedHolder() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        ConnectionHolder holder = new ConnectionHolder(mock(Connection.class)); holder.setSynchronizedWithTransaction(true);
        TransactionSynchronizationManager.bindResource(dataSource, holder);
        assertThatCode(new OutboxTransactionGuard(dataSource)::requireSelectedTransaction).doesNotThrowAnyException();
    }

}
