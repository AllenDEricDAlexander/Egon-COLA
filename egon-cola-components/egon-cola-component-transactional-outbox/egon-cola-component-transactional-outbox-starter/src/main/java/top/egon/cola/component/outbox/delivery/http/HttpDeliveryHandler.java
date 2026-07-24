package top.egon.cola.component.outbox.delivery.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HttpDeliveryHandler implements DeliveryHandler {

    private static final Set<String> RESTRICTED_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding", "connection"
    );

    private final HttpDestinationResolver destinationResolver;
    private final HttpCredentialProvider credentialProvider;
    private final HttpDeliveryClassifier classifier;
    private final Clock clock;

    public HttpDeliveryHandler(
            HttpDestinationResolver destinationResolver,
            HttpCredentialProvider credentialProvider,
            HttpDeliveryClassifier classifier,
            Clock clock
    ) {
        this.destinationResolver = destinationResolver;
        this.credentialProvider = credentialProvider;
        this.classifier = classifier;
        this.clock = clock;
    }

    @Override
    public String channel() {
        return "http";
    }

    @Override
    public void validateDestination(String destination) {
        destinationResolver.resolve(destination);
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        HttpDeliveryTarget target = destinationResolver.resolve(context.destination());
        Duration remaining = Duration.between(clock.instant(), context.deadline());
        if (remaining.isZero() || remaining.isNegative()) {
            return DeliveryResult.retryableFailure(
                    "HTTP_DEADLINE_EXCEEDED",
                    "HTTP_DEADLINE_EXCEEDED"
            );
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(target.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(shorter(target.readTimeout(), remaining));
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        try {
            return restClient
                    .method(HttpMethod.valueOf(target.method().toUpperCase(Locale.ROOT)))
                    .uri(target.uri())
                    .headers(headers -> applyHeaders(headers, target.fixedHeaders(), false))
                    .headers(headers -> applyHeaders(headers, context.headers(), false))
                    .headers(headers -> applyHeaders(
                            headers,
                            credentialProvider.resolveHeaders(context.destination()),
                            true
                    ))
                    .contentType(MediaType.parseMediaType(context.contentType()))
                    .header("Idempotency-Key", context.messageId())
                    .header("X-Egon-Cola-Message-Id", context.messageId())
                    .body(context.payload())
                    .exchange((request, response) -> classifier.classify(response.getStatusCode()));
        } catch (ResourceAccessException exception) {
            String code = isTimeout(exception) ? "HTTP_TIMEOUT" : "HTTP_IO_ERROR";
            return DeliveryResult.retryableFailure(code, exception.getClass().getSimpleName());
        }
    }

    private void applyHeaders(
            HttpHeaders target,
            Map<String, String> source,
            boolean allowSensitive
    ) {
        if (source == null) {
            return;
        }
        source.forEach((name, value) -> {
            String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
            if (RESTRICTED_HEADERS.contains(normalized)) {
                return;
            }
            if (!allowSensitive && OutboxMessageValidator.isForbiddenHeader(name)) {
                return;
            }
            target.set(name, value);
        });
    }

    private Duration shorter(Duration configured, Duration remaining) {
        return configured.compareTo(remaining) <= 0 ? configured : remaining;
    }

    private boolean isTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof java.net.http.HttpTimeoutException
                    || current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
