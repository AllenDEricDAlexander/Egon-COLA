package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record HttpUpstreamRequest(
        ProviderInstance provider,
        String method,
        String pathAndQuery,
        Map<String, List<String>> headers,
        Flux<DataBuffer> body,
        Duration connectTimeout,
        Duration responseHeaderTimeout,
        Duration streamIdleTimeout,
        Optional<Duration> totalTimeout,
        boolean replayable
) {

    public HttpUpstreamRequest(
            ProviderInstance provider,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            Duration timeout) {
        this(
                provider,
                method,
                pathAndQuery,
                headers,
                body,
                timeout,
                timeout,
                timeout,
                Optional.of(timeout),
                false
        );
    }

    public HttpUpstreamRequest(
            ProviderInstance provider,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            Duration timeout,
            boolean replayable) {
        this(
                provider,
                method,
                pathAndQuery,
                headers,
                body,
                timeout,
                timeout,
                timeout,
                Optional.of(timeout),
                replayable
        );
    }

    public HttpUpstreamRequest {
        provider = Objects.requireNonNull(provider, "provider");
        method = Objects.requireNonNull(method, "method");
        pathAndQuery = Objects.requireNonNull(pathAndQuery, "pathAndQuery");
        if (!pathAndQuery.startsWith("/") || pathAndQuery.contains("://")) {
            throw new IllegalArgumentException(
                    "upstream path must be relative to selected provider"
            );
        }
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
        connectTimeout = positive(connectTimeout, "connectTimeout");
        responseHeaderTimeout = positive(
                responseHeaderTimeout,
                "responseHeaderTimeout"
        );
        streamIdleTimeout = positive(
                streamIdleTimeout,
                "streamIdleTimeout"
        );
        totalTimeout = Objects.requireNonNull(totalTimeout, "totalTimeout");
        totalTimeout.ifPresent(value -> positive(value, "totalTimeout"));
    }

    public Duration timeout() {
        return responseHeaderTimeout;
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
