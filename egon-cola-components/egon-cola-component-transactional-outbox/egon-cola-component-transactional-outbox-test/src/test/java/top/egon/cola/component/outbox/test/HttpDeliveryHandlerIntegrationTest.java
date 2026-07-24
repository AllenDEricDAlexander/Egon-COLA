package top.egon.cola.component.outbox.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.delivery.http.DefaultHttpDeliveryClassifier;
import top.egon.cola.component.outbox.delivery.http.HttpCredentialProvider;
import top.egon.cola.component.outbox.delivery.http.HttpDeliveryHandler;
import top.egon.cola.component.outbox.delivery.http.PropertiesHttpDestinationResolver;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.temporaryRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class HttpDeliveryHandlerIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    private WireMockServer wireMock;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void shouldDeliverBodyAndStableIdempotencyHeadersOnTwoHundred() throws Exception {
        wireMock.stubFor(post("/callback").willReturn(noContent()));

        DeliveryResult result = handler(Duration.ofSeconds(2), HttpCredentialProvider.none())
                .deliver(context("message-1", NOW.plusSeconds(10)));

        assertThat(result).isEqualTo(DeliveryResult.success());
        wireMock.verify(postRequestedFor(urlEqualTo("/callback"))
                .withHeader("Idempotency-Key", equalTo("message-1"))
                .withHeader("X-Egon-Cola-Message-Id", equalTo("message-1"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("{\"orderId\":\"O-1\"}")));
    }

    @Test
    void shouldClassifyThrottleServerAndClientFailures() throws Exception {
        assertThat(deliverStatus(429).kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(deliverStatus(503).kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(deliverStatus(400).kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
    }

    @Test
    void shouldNotFollowRedirects() throws Exception {
        wireMock.stubFor(post("/callback")
                .willReturn(temporaryRedirect("/credential-capture")));

        DeliveryResult result = handler(Duration.ofSeconds(2), HttpCredentialProvider.none())
                .deliver(context("message-1", NOW.plusSeconds(10)));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        wireMock.verify(0, postRequestedFor(urlEqualTo("/credential-capture")));
    }

    @Test
    void shouldRetryTimeoutWithoutReadingResponseBody() throws Exception {
        wireMock.stubFor(post("/callback").willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(503)
                .withBody("Authorization: secret-value")));

        DeliveryResult result = handler(Duration.ofMillis(50), HttpCredentialProvider.none())
                .deliver(context("message-1", NOW.plusSeconds(10)));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(result.message()).doesNotContain("secret-value");
    }

    @Test
    void shouldApplyCredentialsInMemoryWithoutChangingDeliveryContext() throws Exception {
        wireMock.stubFor(post("/callback").willReturn(noContent()));
        DeliveryContext context = context("message-1", NOW.plusSeconds(10));

        DeliveryResult result = handler(
                Duration.ofSeconds(2),
                destination -> Map.of("Authorization", "Bearer in-memory-secret")
        ).deliver(context);

        assertThat(result).isEqualTo(DeliveryResult.success());
        assertThat(context.headers()).doesNotContainKey("Authorization");
        wireMock.verify(postRequestedFor(urlEqualTo("/callback"))
                .withHeader("Authorization", equalTo("Bearer in-memory-secret")));
    }

    @Test
    void shouldRejectExpiredDeadlineWithoutSendingRequest() throws Exception {
        DeliveryResult result = handler(Duration.ofSeconds(2), HttpCredentialProvider.none())
                .deliver(context("message-1", NOW));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(result.code()).isEqualTo("HTTP_DEADLINE_EXCEEDED");
        wireMock.verify(0, postRequestedFor(urlEqualTo("/callback")));
    }

    private DeliveryResult deliverStatus(int status) throws Exception {
        wireMock.resetAll();
        wireMock.stubFor(post("/callback").willReturn(aResponse().withStatus(status)));
        return handler(Duration.ofSeconds(2), HttpCredentialProvider.none())
                .deliver(context("message-1", NOW.plusSeconds(10)));
    }

    private HttpDeliveryHandler handler(
            Duration readTimeout,
            HttpCredentialProvider credentialProvider
    ) {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        TransactionalOutboxProperties.HttpDestination destination =
                new TransactionalOutboxProperties.HttpDestination();
        destination.setUri(URI.create(wireMock.baseUrl() + "/callback"));
        destination.setConnectTimeout(Duration.ofSeconds(1));
        destination.setReadTimeout(readTimeout);
        destination.getFixedHeaders().put("X-Source", "orders");
        properties.getHttp().getDestinations().put("order-callback", destination);
        return new HttpDeliveryHandler(
                new PropertiesHttpDestinationResolver(properties),
                credentialProvider,
                new DefaultHttpDeliveryClassifier(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private DeliveryContext context(String messageId, Instant deadline) {
        return new DeliveryContext(
                messageId,
                "http",
                "order-callback",
                "{\"orderId\":\"O-1\"}",
                "application/json",
                "1",
                Map.of("X-Tenant", "tenant-1"),
                "trace-1",
                1,
                10,
                deadline
        );
    }
}
