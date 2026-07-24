package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.outbox.delivery.http.HttpDeliveryHandler;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitDeliveryHandler;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.store.OutboxStore;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxOptionalAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OutboxMetricsAutoConfiguration.class,
                    TransactionalOutboxAutoConfiguration.class,
                    OutboxHttpAutoConfiguration.class,
                    OutboxRabbitAutoConfiguration.class
            ))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(
                    PlatformTransactionManager.class,
                    () -> mock(PlatformTransactionManager.class)
            )
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(OutboxStore.class, () -> mock(OutboxStore.class))
            .withPropertyValues(
                    "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                    "egon.cola.component.transactional-outbox.polling.enabled=false"
            );

    @Test
    void shouldKeepOptionalHandlersDisabledByDefault() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(HttpDeliveryHandler.class)
                .doesNotHaveBean(RabbitDeliveryHandler.class));
    }

    @Test
    void shouldCreateHttpHandlerWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.transactional-outbox.http.enabled=true",
                        "egon.cola.component.transactional-outbox.http.destinations.orders.uri=http://localhost/callback"
                )
                .run(context -> assertThat(context).hasSingleBean(HttpDeliveryHandler.class));
    }

    @Test
    void shouldFailRabbitStartupWithoutPublisherSafety() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        contextRunner
                .withBean(RabbitTemplate.class, () -> rabbitTemplate)
                .withPropertyValues(rabbitProperties())
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(OutboxConfigurationException.class);
                });
    }

    @Test
    void shouldCreateRabbitHandlerWithConfirmsReturnsAndMandatoryPublishing() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.isPublisherConfirms()).thenReturn(true);
        when(connectionFactory.isPublisherReturns()).thenReturn(true);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(true);

        contextRunner
                .withBean(RabbitTemplate.class, () -> rabbitTemplate)
                .withPropertyValues(rabbitProperties())
                .run(context -> assertThat(context).hasSingleBean(RabbitDeliveryHandler.class));
    }

    private String[] rabbitProperties() {
        return new String[]{
                "egon.cola.component.transactional-outbox.rabbitmq.enabled=true",
                "egon.cola.component.transactional-outbox.rabbitmq.destinations.orders.exchange=orders.events",
                "egon.cola.component.transactional-outbox.rabbitmq.destinations.orders.routing-key=order.created"
        };
    }
}
