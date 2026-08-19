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
import top.egon.cola.component.outbox.annotation.TransactionalMessage;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.autoconfigure.OutboxHttpAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxMetricsAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxRabbitAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.exception.OutboxMessageResolutionException;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalMessageAnnotationSampleTest extends PostgresqlOutboxTestSupport {

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
    void shouldCommitOneAnnotatedCallAndRollBackNullExpression() {
        contextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            AnnotatedOrderApplicationService service =
                    context.getBean(AnnotatedOrderApplicationService.class);

            CreateOrderResult result = service.createOrder("O-1");

            assertThat(result.orderId()).isEqualTo("O-1");
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from outbox_sample_order where id = 'O-1'",
                    Integer.class
            )).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from egon_cola_outbox_message",
                    Integer.class
            )).isEqualTo(1);

            assertThatThrownBy(() -> service.createWithoutMessage("O-2"))
                    .isInstanceOf(OutboxMessageResolutionException.class);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from outbox_sample_order where id = 'O-2'",
                    Integer.class
            )).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from egon_cola_outbox_message",
                    Integer.class
            )).isEqualTo(1);
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
                .withUserConfiguration(AnnotationSampleConfiguration.class)
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
    static class AnnotationSampleConfiguration {

        @Bean
        AnnotatedOrderApplicationService annotatedOrderApplicationService(
                @Qualifier("applicationJdbcTemplate") JdbcTemplate jdbcTemplate
        ) {
            return new AnnotatedOrderApplicationService(jdbcTemplate);
        }

        @Bean
        DeliveryHandler sampleDeliveryHandler() {
            return new SampleDeliveryHandler();
        }
    }

    static class AnnotatedOrderApplicationService {

        private final JdbcTemplate jdbcTemplate;

        AnnotatedOrderApplicationService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @TransactionalMessage(message = "#result.outboxMessage()")
        public CreateOrderResult createOrder(String orderId) {
            jdbcTemplate.update(
                    "insert into outbox_sample_order(id) values (?)",
                    orderId
            );
            return new CreateOrderResult(
                    orderId,
                    OutboxMessage.builder()
                            .idempotencyKey("order:annotated-created:" + orderId)
                            .channel("sample")
                            .destination("order-created-v1")
                            .payload(new OrderCreatedEvent(orderId))
                            .build()
            );
        }

        @TransactionalMessage(message = "#result.outboxMessage()")
        public CreateOrderResult createWithoutMessage(String orderId) {
            jdbcTemplate.update(
                    "insert into outbox_sample_order(id) values (?)",
                    orderId
            );
            return null;
        }
    }

    record OrderCreatedEvent(String orderId) {
    }

    record CreateOrderResult(String orderId, OutboxMessage outboxMessage) {
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
