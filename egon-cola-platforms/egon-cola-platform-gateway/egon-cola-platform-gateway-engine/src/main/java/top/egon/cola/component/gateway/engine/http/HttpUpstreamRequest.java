package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record HttpUpstreamRequest(
        ProviderInstance provider,
        String method,
        String pathAndQuery,
        Map<String, List<String>> headers,
        Flux<DataBuffer> body,
        Duration timeout
) {

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
        timeout = Objects.requireNonNull(timeout, "timeout");
    }
}
