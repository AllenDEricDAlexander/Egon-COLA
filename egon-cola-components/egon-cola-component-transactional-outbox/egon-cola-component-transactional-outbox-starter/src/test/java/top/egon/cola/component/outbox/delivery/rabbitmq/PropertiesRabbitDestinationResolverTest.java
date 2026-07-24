package top.egon.cola.component.outbox.delivery.rabbitmq;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesRabbitDestinationResolverTest {

    @Test
    void shouldSnapshotDestinationAndUseGlobalConfirmTimeoutByDefault() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getRabbitmq().setConfirmTimeout(Duration.ofSeconds(3));
        TransactionalOutboxProperties.RabbitDestination configured =
                new TransactionalOutboxProperties.RabbitDestination();
        configured.setExchange("orders.events");
        configured.setRoutingKey("order.created");
        configured.getFixedHeaders().put("X-Source", "orders");
        properties.getRabbitmq().getDestinations().put("order-events", configured);

        PropertiesRabbitDestinationResolver resolver =
                new PropertiesRabbitDestinationResolver(properties);
        configured.setExchange("changed");

        RabbitDeliveryTarget target = resolver.resolve("order-events");
        assertThat(target.exchange()).isEqualTo("orders.events");
        assertThat(target.routingKey()).isEqualTo("order.created");
        assertThat(target.confirmTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(target.fixedHeaders()).containsEntry("X-Source", "orders");
    }

    @Test
    void shouldPreferDestinationConfirmTimeoutAndRejectUnknownLogicalName() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        TransactionalOutboxProperties.RabbitDestination configured =
                new TransactionalOutboxProperties.RabbitDestination();
        configured.setExchange("orders.events");
        configured.setRoutingKey("order.created");
        configured.setConfirmTimeout(Duration.ofMillis(250));
        properties.getRabbitmq().getDestinations().put("order-events", configured);
        PropertiesRabbitDestinationResolver resolver =
                new PropertiesRabbitDestinationResolver(properties);

        assertThat(resolver.resolve("order-events").confirmTimeout())
                .isEqualTo(Duration.ofMillis(250));
        assertThatThrownBy(() -> resolver.resolve("missing"))
                .isInstanceOf(OutboxValidationException.class)
                .hasMessageContaining("missing")
                .hasMessageNotContaining("orders.events");
    }
}
