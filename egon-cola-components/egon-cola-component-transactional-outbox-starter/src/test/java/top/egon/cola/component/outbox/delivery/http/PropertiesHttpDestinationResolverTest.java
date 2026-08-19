package top.egon.cola.component.outbox.delivery.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesHttpDestinationResolverTest {

    @Test
    void shouldSnapshotAndResolveConfiguredLogicalDestination() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        TransactionalOutboxProperties.HttpDestination configured =
                new TransactionalOutboxProperties.HttpDestination();
        configured.setUri(URI.create("https://example.test/callback"));
        configured.getFixedHeaders().put("X-Source", "orders");
        properties.getHttp().getDestinations().put("order-callback", configured);

        PropertiesHttpDestinationResolver resolver =
                new PropertiesHttpDestinationResolver(properties);
        configured.setUri(URI.create("https://changed.test"));

        HttpDeliveryTarget target = resolver.resolve("order-callback");
        assertThat(target.uri()).isEqualTo(URI.create("https://example.test/callback"));
        assertThat(target.fixedHeaders()).containsEntry("X-Source", "orders");
        assertThatThrownBy(() -> target.fixedHeaders().put("X", "Y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectUnknownLogicalDestinationWithoutExposingUrls() {
        PropertiesHttpDestinationResolver resolver =
                new PropertiesHttpDestinationResolver(new TransactionalOutboxProperties());

        assertThatThrownBy(() -> resolver.resolve("missing-callback"))
                .isInstanceOf(OutboxValidationException.class)
                .hasMessageContaining("missing-callback")
                .hasMessageNotContaining("http");
    }
}
