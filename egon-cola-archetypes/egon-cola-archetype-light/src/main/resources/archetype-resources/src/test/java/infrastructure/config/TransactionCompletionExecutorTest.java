package ${package}.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionCompletionExecutorTest {
    private final TransactionCompletionExecutor executor = new TransactionCompletionExecutor();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(
            new DataSourceTransactionManager(new DriverManagerDataSource(
                    "jdbc:h2:mem:transaction-completion;DB_CLOSE_DELAY=-1", "sa", "")));

    @Test
    void runs_after_commit_action_once_only_on_commit() {
        AtomicInteger calls = new AtomicInteger();

        transactionTemplate.executeWithoutResult(status -> executor.executeAfterCommit(calls::incrementAndGet));
        assertEquals(1, calls.get());

        transactionTemplate.executeWithoutResult(status -> {
            executor.executeAfterCommit(calls::incrementAndGet);
            status.setRollbackOnly();
        });
        assertEquals(1, calls.get());
    }

    @Test
    void runs_after_rollback_action_once_only_on_rollback() {
        AtomicInteger calls = new AtomicInteger();

        transactionTemplate.executeWithoutResult(status -> executor.executeAfterRollback(calls::incrementAndGet));
        assertEquals(0, calls.get());

        transactionTemplate.executeWithoutResult(status -> {
            executor.executeAfterRollback(calls::incrementAndGet);
            status.setRollbackOnly();
        });
        assertEquals(1, calls.get());
    }
}
