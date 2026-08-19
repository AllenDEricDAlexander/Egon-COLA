package top.egon.cola.component.outbox.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.OutboxHttpAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxMetricsAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxRabbitAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalOutboxDirectApiSampleTest extends PostgresqlOutboxTestSupport {

    @BeforeEach
    void prepareBusinessTable() {
        jdbcTemplate.execute("""
                create table if not exists outbox_sample_order (
                    id varchar(64) primary key
                )
                """);
        jdbcTemplate.execute("truncate table outbox_sample_order");
    }

    @Test
    void shouldCommitBusinessAndOutboxRowsThroughRecommendedDirectApi() {
        contextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            OrderApplicationService service = context.getBean(OrderApplicationService.class);

            OutboxReceipt created = service.createOrder("O-1");
            OutboxReceipt existing = service.createOrder("O-1");

            assertThat(created.created()).isTrue();
            assertThat(created.messageId()).isNotBlank();
            assertThat(existing.created()).isFalse();
            assertThat(existing.messageId()).isEqualTo(created.messageId());
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from outbox_sample_order",
                    Integer.class
            )).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from egon_cola_outbox_message",
                    Integer.class
            )).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("""
                    select destination
                    from egon_cola_outbox_message
                    where message_id = ?
                    """, String.class, created.messageId())).isEqualTo("order-created-v1");
        });
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        OutboxMetricsAutoConfiguration.class,
                        TransactionalOutboxAutoConfiguration.class,
                        OutboxHttpAutoConfiguration.class,
                        OutboxRabbitAutoConfiguration.class
                ))
                .withUserConfiguration(DirectSampleConfiguration.class)
                .withBean("dataSource", DataSource.class, () -> dataSource)
                .withBean(
                        "transactionManager",
                        PlatformTransactionManager.class,
                        () -> transactionManager
                )
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(
                        "applicationJdbcTemplate",
                        JdbcTemplate.class,
                        () -> new JdbcTemplate(dataSource)
                )
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                        "egon.cola.component.transactional-outbox.polling.enabled=false"
                );
    }

    @Configuration(proxyBeanMethods = false)
    static class DirectSampleConfiguration {

        @Bean
        OrderApplicationService orderApplicationService(
                @Qualifier("applicationJdbcTemplate") JdbcTemplate jdbcTemplate,
                TransactionalOutbox transactionalOutbox
        ) {
            return new OrderApplicationService(jdbcTemplate, transactionalOutbox);
        }

        @Bean
        DeliveryHandler sampleDeliveryHandler() {
            return new SampleDeliveryHandler();
        }
    }

    static class OrderApplicationService {

        private final JdbcTemplate jdbcTemplate;
        private final TransactionalOutbox transactionalOutbox;

        OrderApplicationService(
                JdbcTemplate jdbcTemplate,
                TransactionalOutbox transactionalOutbox
        ) {
            this.jdbcTemplate = jdbcTemplate;
            this.transactionalOutbox = transactionalOutbox;
        }

        @Transactional
        public OutboxReceipt createOrder(String orderId) {
            jdbcTemplate.update(
                    "insert into outbox_sample_order(id) values (?) on conflict do nothing",
                    orderId
            );
            return transactionalOutbox.enqueue(OutboxMessage.builder()
                    .idempotencyKey("order:created:" + orderId)
                    .channel("sample")
                    .destination("order-created-v1")
                    .payload(new OrderCreatedEvent(orderId))
                    .schemaVersion("1")
                    .build());
        }
    }

    record OrderCreatedEvent(String orderId) {
    }

    static class SampleDeliveryHandler implements DeliveryHandler {

        @Override
        public String channel() {
            return "sample";
        }

        @Override
        public void validateDestination(String destination) {
            if (!"order-created-v1".equals(destination)) {
                throw new OutboxValidationException("Unknown sample destination");
            }
        }

        @Override
        public DeliveryResult deliver(DeliveryContext context) {
            return DeliveryResult.success();
        }
    }
}
