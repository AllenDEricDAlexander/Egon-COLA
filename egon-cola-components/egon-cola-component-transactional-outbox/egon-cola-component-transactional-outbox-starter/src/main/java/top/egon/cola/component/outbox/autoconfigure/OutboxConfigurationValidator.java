package top.egon.cola.component.outbox.autoconfigure;

import top.egon.cola.component.outbox.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OutboxConfigurationValidator {

    private static final Duration LEASE_SAFETY_MARGIN = Duration.ofSeconds(1);
    private static final Set<String> STANDARD_HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"
    );

    public void validate(TransactionalOutboxProperties properties) {
        if (properties == null) {
            throw invalid("properties");
        }
        validateCore(properties);
        validateHttp(properties.getHttp());
        validateRabbitmq(properties.getRabbitmq());
    }

    private void validateCore(TransactionalOutboxProperties properties) {
        positive(properties.getPolling().getFixedDelay(), "polling.fixed-delay");
        positive(properties.getPolling().getBatchSize(), "polling.batch-size");
        positive(properties.getPolling().getConcurrency(), "polling.concurrency");
        positive(properties.getDelivery().getTimeout(), "delivery.timeout");
        positive(properties.getDelivery().getLeaseDuration(), "delivery.lease-duration");
        positive(properties.getDelivery().getQueueCapacity(), "delivery.queue-capacity");
        positive(properties.getRetry().getMaxAttempts(), "retry.max-attempts");
        positive(properties.getRetry().getInitialDelay(), "retry.initial-delay");
        positive(properties.getRetry().getMaxDelay(), "retry.max-delay");
        if (!Double.isFinite(properties.getRetry().getMultiplier())
                || properties.getRetry().getMultiplier() < 1.0) {
            throw invalid("retry.multiplier");
        }
        if (!Double.isFinite(properties.getRetry().getJitter())
                || properties.getRetry().getJitter() < 0.0
                || properties.getRetry().getJitter() > 1.0) {
            throw invalid("retry.jitter");
        }
        if (properties.getRetry().getMaxDelay().compareTo(properties.getRetry().getInitialDelay()) < 0) {
            throw invalid("retry.max-delay");
        }
        positive(properties.getPayload().getMaxBytes(), "payload.max-bytes");
        positive(properties.getPayload().getMaxHeaderCount(), "payload.max-header-count");
        positive(properties.getPayload().getMaxHeaderBytes(), "payload.max-header-bytes");
        positive(properties.getCleanup().getSuccessRetention(), "cleanup.success-retention");
        positive(properties.getCleanup().getFixedDelay(), "cleanup.fixed-delay");
        positive(properties.getCleanup().getBatchSize(), "cleanup.batch-size");
        positive(properties.getShutdown().getGracePeriod(), "shutdown.grace-period");

        Duration minimumLease = properties.getDelivery().getTimeout()
                .plus(properties.getPolling().getFixedDelay())
                .plus(LEASE_SAFETY_MARGIN);
        if (properties.getDelivery().getLeaseDuration().compareTo(minimumLease) <= 0) {
            throw invalid("delivery.lease-duration");
        }
        if (properties.getNodeId() != null && properties.getNodeId().length() > 80) {
            throw invalid("node-id");
        }
    }

    private void validateHttp(TransactionalOutboxProperties.Http http) {
        http.getDestinations().forEach((key, destination) -> {
            requireDestinationKey(key, "http");
            if (destination == null) {
                throw invalidDestination("http", key);
            }
            URI uri = destination.getUri();
            String scheme = uri == null ? null : uri.getScheme();
            if (scheme == null
                    || !Set.of("http", "https").contains(scheme.toLowerCase(Locale.ROOT))
                    || uri.getUserInfo() != null) {
                throw invalidDestination("http", key);
            }
            String method = destination.getMethod();
            if (method == null || !STANDARD_HTTP_METHODS.contains(method.toUpperCase(Locale.ROOT))) {
                throw invalidDestination("http", key);
            }
            positiveDestination(destination.getConnectTimeout(), "http", key);
            positiveDestination(destination.getReadTimeout(), "http", key);
            validateFixedHeaders(destination.getFixedHeaders(), "http", key);
        });
    }

    private void validateRabbitmq(TransactionalOutboxProperties.Rabbitmq rabbitmq) {
        positive(rabbitmq.getConfirmTimeout(), "rabbitmq.confirm-timeout");
        rabbitmq.getDestinations().forEach((key, destination) -> {
            requireDestinationKey(key, "rabbitmq");
            if (destination == null
                    || isBlank(destination.getExchange())
                    || isBlank(destination.getRoutingKey())
                    || !destination.isMandatory()) {
                throw invalidDestination("rabbitmq", key);
            }
            Duration confirmTimeout = destination.getConfirmTimeout() == null
                    ? rabbitmq.getConfirmTimeout()
                    : destination.getConfirmTimeout();
            positiveDestination(confirmTimeout, "rabbitmq", key);
            validateFixedHeaders(destination.getFixedHeaders(), "rabbitmq", key);
        });
    }

    private void validateFixedHeaders(Map<String, String> headers, String channel, String key) {
        if (headers == null) {
            throw invalidDestination(channel, key);
        }
        headers.forEach((name, value) -> {
            if (isBlank(name) || value == null || OutboxMessageValidator.isForbiddenHeader(name)) {
                throw invalidDestination(channel, key);
            }
        });
    }

    private void requireDestinationKey(String key, String channel) {
        if (isBlank(key)) {
            throw invalid(channel + ".destinations");
        }
    }

    private void positive(Duration value, String property) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw invalid(property);
        }
    }

    private void positive(org.springframework.util.unit.DataSize value, String property) {
        if (value == null || value.toBytes() <= 0) {
            throw invalid(property);
        }
    }

    private void positive(int value, String property) {
        if (value <= 0) {
            throw invalid(property);
        }
    }

    private void positiveDestination(Duration value, String channel, String key) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw invalidDestination(channel, key);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OutboxConfigurationException invalid(String property) {
        return new OutboxConfigurationException("Invalid transactional outbox configuration: " + property);
    }

    private OutboxConfigurationException invalidDestination(String channel, String key) {
        return new OutboxConfigurationException(
                "Invalid transactional outbox " + channel + " destination: " + key
        );
    }
}
