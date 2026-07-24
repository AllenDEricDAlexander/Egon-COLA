package top.egon.cola.component.outbox.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.api.UuidOutboxIdGenerator;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.event.OutboxCommittedEvent;
import top.egon.cola.component.outbox.exception.OutboxTransactionMismatchException;
import top.egon.cola.component.outbox.exception.OutboxTransactionRequiredException;
import top.egon.cola.component.outbox.serialization.JacksonOutboxMessageSerializer;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;
import top.egon.cola.component.outbox.transaction.DefaultTransactionalOutbox;
import top.egon.cola.component.outbox.transaction.OutboxAfterCommitBuffer;
import top.egon.cola.component.outbox.transaction.OutboxTransactionGuard;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionalOutboxTransactionIntegrationTest extends PostgresqlOutboxTestSupport {

    private final List<OutboxCommittedEvent> committedEvents = new ArrayList<>();
    private TransactionTemplate transactionTemplate;
    private TransactionalOutbox outbox;

    @BeforeEach
    void setUpTransactionFixture() {
        jdbcTemplate.execute("""
                create table if not exists outbox_test_order (
                    id bigint primary key,
                    state varchar(32) not null
                )
                """);
        jdbcTemplate.execute("truncate table outbox_test_order");
        committedEvents.clear();
        transactionTemplate = new TransactionTemplate(transactionManager);
        outbox = createOutbox(
                new PostgresqlJdbcOutboxStore(
                        jdbcTemplate,
                        new NamedParameterJdbcTemplate(dataSource),
                        objectMapper,
                        transactionManager
                ),
                dataSource,
                event -> committedEvents.add((OutboxCommittedEvent) event)
        );
    }

    @Test
    void shouldCommitBusinessAndOutboxRowsAndPublishAfterCommit() {
        OutboxReceipt receipt = transactionTemplate.execute(status -> {
            jdbcTemplate.update("insert into outbox_test_order(id, state) values (1, 'CREATED')");
            return outbox.enqueue(message("order-1"));
        });

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_test_order", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from egon_cola_outbox_message", Integer.class)).isEqualTo(1);
        assertThat(committedEvents).singleElement()
                .extracting(OutboxCommittedEvent::messageIds)
                .isEqualTo(List.of(receipt.messageId()));
    }

    @Test
    void shouldRollBackBusinessAndOutboxRowsWithoutPublishing() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("insert into outbox_test_order(id, state) values (1, 'CREATED')");
            outbox.enqueue(message("order-1"));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_test_order", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from egon_cola_outbox_message", Integer.class)).isZero();
        assertThat(committedEvents).isEmpty();
    }

    @Test
    void shouldRollBackBusinessDataWhenOutboxStoreFails() {
        OutboxStore failingStore = mock(OutboxStore.class);
        when(failingStore.enqueue(any())).thenThrow(new IllegalStateException("storage failed"));
        TransactionalOutbox failingOutbox = createOutbox(
                failingStore,
                dataSource,
                event -> committedEvents.add((OutboxCommittedEvent) event)
        );

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("insert into outbox_test_order(id, state) values (1, 'CREATED')");
            failingOutbox.enqueue(message("order-1"));
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_test_order", Integer.class)).isZero();
        assertThat(committedEvents).isEmpty();
    }

    @Test
    void shouldRejectEnqueueOutsideTransaction() {
        assertThatThrownBy(() -> outbox.enqueue(message("order-1")))
                .isInstanceOf(OutboxTransactionRequiredException.class);
    }

    @Test
    void shouldRejectTransactionManagedByAnotherDataSourceBean() {
        DelegatingDataSource otherDataSource = new DelegatingDataSource(dataSource);
        TransactionTemplate otherTransaction = new TransactionTemplate(
                new DataSourceTransactionManager(otherDataSource)
        );

        assertThatThrownBy(() -> otherTransaction.executeWithoutResult(
                status -> outbox.enqueue(message("order-1"))))
                .isInstanceOf(OutboxTransactionMismatchException.class);
    }

    private TransactionalOutbox createOutbox(
            OutboxStore store,
            javax.sql.DataSource selectedDataSource,
            ApplicationEventPublisher publisher
    ) {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        return new DefaultTransactionalOutbox(
                new OutboxMessageValidator(objectMapper, 1_048_576, 64, 16_384),
                new JacksonOutboxMessageSerializer(objectMapper),
                new UuidOutboxIdGenerator(),
                objectMapper,
                new OutboxTransactionGuard(selectedDataSource),
                store,
                new DeliveryHandlerRegistry(List.of(new NoOpHttpDeliveryHandler())),
                new OutboxAfterCommitBuffer(publisher),
                properties
        );
    }

    private OutboxMessage message(String idempotencyKey) {
        return OutboxMessage.builder()
                .idempotencyKey(idempotencyKey)
                .channel("http")
                .destination("orders")
                .payload(Map.of("orderId", 1))
                .build();
    }

    private static final class NoOpHttpDeliveryHandler implements DeliveryHandler {

        @Override
        public String channel() {
            return "http";
        }

        @Override
        public void validateDestination(String destination) {
        }

        @Override
        public DeliveryResult deliver(DeliveryContext context) {
            return DeliveryResult.success();
        }
    }
}
