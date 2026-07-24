package top.egon.cola.component.outbox.autoconfigure;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxConfigurationValidatorTest {

    private final OutboxConfigurationValidator validator = new OutboxConfigurationValidator();

    @Test
    void shouldRejectLeaseThatCannotCoverDeliveryWindow() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getDelivery().setTimeout(Duration.ofSeconds(10));
        properties.getDelivery().setLeaseDuration(Duration.ofSeconds(11));
        properties.getPolling().setFixedDelay(Duration.ofSeconds(1));

        assertThatThrownBy(() -> validator.validate(properties))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessageContaining("lease-duration");
    }

    @Test
    void shouldRejectInvalidRetryAndCapacityRanges() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getRetry().setJitter(1.1);

        assertThatThrownBy(() -> validator.validate(properties))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessageContaining("retry.jitter");
    }

    @Test
    void shouldRejectSensitiveFixedHeaderWithoutExposingDestinationValue() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        TransactionalOutboxProperties.HttpDestination destination =
                new TransactionalOutboxProperties.HttpDestination();
        destination.setUri(URI.create("https://orders.test/callback"));
        destination.getFixedHeaders().put("Authorization", "secret");
        properties.getHttp().getDestinations().put("order-callback", destination);

        assertThatThrownBy(() -> validator.validate(properties))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessageContaining("order-callback")
                .hasMessageNotContaining("orders.test")
                .hasMessageNotContaining("secret");
    }
}
