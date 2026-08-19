package top.egon.cola.component.outbox.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalOutboxPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void shouldExposeSafeDefaults() {
        contextRunner.run(context -> {
            TransactionalOutboxProperties properties =
                    context.getBean(TransactionalOutboxProperties.class);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getAnnotation().isEnabled()).isTrue();
            assertThat(properties.getAnnotation().getOrder())
                    .isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
            assertThat(properties.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(1));
            assertThat(properties.getPolling().getBatchSize()).isEqualTo(100);
            assertThat(properties.getPolling().getConcurrency()).isEqualTo(4);
            assertThat(properties.getDelivery().getTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getDelivery().getLeaseDuration()).isEqualTo(Duration.ofSeconds(60));
            assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(10);
            assertThat(properties.getHttp().isEnabled()).isFalse();
            assertThat(properties.getRabbitmq().isEnabled()).isFalse();
            assertThat(properties.getCleanup().isEnabled()).isFalse();
        });
    }

    @Test
    void shouldBindNestedDestinationsAndBeanNames() {
        contextRunner.withPropertyValues(
                "egon.cola.component.transactional-outbox.storage.data-source-bean-name=businessDataSource",
                "egon.cola.component.transactional-outbox.storage.transaction-manager-bean-name=businessTx",
                "egon.cola.component.transactional-outbox.http.enabled=true",
                "egon.cola.component.transactional-outbox.http.destinations.order-callback.uri=https://orders.test/callback",
                "egon.cola.component.transactional-outbox.http.destinations.order-callback.method=PUT",
                "egon.cola.component.transactional-outbox.rabbitmq.destinations.order-created.exchange=order.events",
                "egon.cola.component.transactional-outbox.rabbitmq.destinations.order-created.routing-key=order.created"
        ).run(context -> {
            TransactionalOutboxProperties properties =
                    context.getBean(TransactionalOutboxProperties.class);

            assertThat(properties.getStorage().getDataSourceBeanName())
                    .isEqualTo("businessDataSource");
            assertThat(properties.getStorage().getTransactionManagerBeanName())
                    .isEqualTo("businessTx");
            assertThat(properties.getHttp().getDestinations())
                    .containsKey("order-callback");
            assertThat(properties.getRabbitmq().getDestinations())
                    .containsKey("order-created");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TransactionalOutboxProperties.class)
    static class PropertiesConfiguration {
    }
}
