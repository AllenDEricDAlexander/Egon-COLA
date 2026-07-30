package top.egon.cola.component.gateway.engine.http.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogDirection;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogEvent;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogTap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public record GatewayHttpProxyContext(
        HttpUpstreamAdapter adapter,
        ProviderInstance provider,
        String method,
        String pathAndQuery,
        Map<String, List<String>> headers,
        Flux<DataBuffer> body,
        EffectiveGatewayTransportPolicy policy,
        int bodyLogSampleBytes,
        Consumer<GatewayBodyLogEvent> bodyLogObserver
) {

    public GatewayHttpProxyContext(
            HttpUpstreamAdapter adapter,
            ProviderInstance provider,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            EffectiveGatewayTransportPolicy policy) {
        this(
                adapter,
                provider,
                method,
                pathAndQuery,
                headers,
                body,
                policy,
                GatewayBodyLogTap.DEFAULT_SAMPLE_BYTES,
                ignored -> {
                }
        );
    }

    public GatewayHttpProxyContext {
        adapter = Objects.requireNonNull(adapter, "adapter");
        provider = Objects.requireNonNull(provider, "provider");
        method = Objects.requireNonNull(method, "method");
        pathAndQuery = Objects.requireNonNull(pathAndQuery, "pathAndQuery");
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
        policy = Objects.requireNonNull(policy, "policy");
        if (bodyLogSampleBytes < 1) {
            throw new IllegalArgumentException(
                    "bodyLogSampleBytes must be positive"
            );
        }
        bodyLogObserver = Objects.requireNonNull(
                bodyLogObserver,
                "bodyLogObserver"
        );
    }

    public Flux<DataBuffer> observeBody(
            Flux<DataBuffer> source,
            GatewayBodyLogDirection direction,
            Map<String, List<String>> bodyHeaders) {
        return GatewayBodyLogTap.tap(
                source,
                policy.bodyLogEnabled(),
                firstHeader(bodyHeaders, "content-type"),
                direction,
                bodyLogSampleBytes,
                bodyLogObserver
        );
    }

    private static String firstHeader(
            Map<String, List<String>> headers,
            String expectedName) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(expectedName)
                    && entry.getValue() != null
                    && !entry.getValue().isEmpty()) {
                return entry.getValue().getFirst();
            }
        }
        return null;
    }
}
