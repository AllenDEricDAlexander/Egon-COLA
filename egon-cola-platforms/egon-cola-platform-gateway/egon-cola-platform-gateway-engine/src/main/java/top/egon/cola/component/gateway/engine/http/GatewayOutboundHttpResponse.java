package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streaming HTTP response plus an idempotent action for abandoning resources
 * before, during, or after body subscription.
 *
 * <p>The body producer owns buffers that have not been emitted. It must create
 * pooled elements lazily or release them when its subscription is cancelled.
 * The downstream consumer owns every emitted buffer until it transfers or
 * releases that buffer. A publisher of pre-created pooled buffers, such as a
 * plain {@code Flux.just}, cannot satisfy this cancellation contract.</p>
 */
public final class GatewayOutboundHttpResponse {

    private final int status;

    private final Map<String, List<String>> headers;

    private final Flux<DataBuffer> body;

    private final Runnable abandonAction;

    private final AtomicBoolean abandoned = new AtomicBoolean();

    public GatewayOutboundHttpResponse(
            int status,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body) {
        this(status, headers, body, () -> {
        });
    }

    GatewayOutboundHttpResponse(
            int status,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            Runnable abandonAction) {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("invalid HTTP status");
        }
        this.status = status;
        this.headers = Map.copyOf(
                Objects.requireNonNull(headers, "headers")
        );
        this.body = Objects.requireNonNull(body, "body");
        this.abandonAction = Objects.requireNonNull(
                abandonAction,
                "abandonAction"
        );
    }

    public int status() {
        return status;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public Flux<DataBuffer> body() {
        return body;
    }

    GatewayOutboundHttpResponse withBody(Flux<DataBuffer> replacement) {
        return withHeadersAndBody(headers, replacement);
    }

    GatewayOutboundHttpResponse withHeadersAndBody(
            Map<String, List<String>> replacementHeaders,
            Flux<DataBuffer> replacementBody) {
        return new GatewayOutboundHttpResponse(
                status,
                replacementHeaders,
                replacementBody,
                this::abandon
        );
    }

    GatewayOutboundHttpResponse onAbandon(Runnable action) {
        Objects.requireNonNull(action, "action");
        return new GatewayOutboundHttpResponse(
                status,
                headers,
                body,
                () -> {
                    try {
                        abandon();
                    } finally {
                        action.run();
                    }
                }
        );
    }

    void abandon() {
        if (abandoned.compareAndSet(false, true)) {
            abandonAction.run();
        }
    }

    public static GatewayOutboundHttpResponse text(
            int status,
            String content) {
        return new GatewayOutboundHttpResponse(
                status,
                Map.of("content-type", List.of("text/plain; charset=UTF-8")),
                Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(
                        content.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                ))
        );
    }
}
