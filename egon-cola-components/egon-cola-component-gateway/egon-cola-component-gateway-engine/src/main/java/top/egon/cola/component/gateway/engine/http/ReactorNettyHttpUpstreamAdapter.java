package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReactorNettyHttpUpstreamAdapter
        implements HttpUpstreamAdapter, AutoCloseable {

    private static final Set<String> FORBIDDEN = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "x-forwarded-for",
            "x-forwarded-host",
            "x-forwarded-proto",
            "x-egon-principal",
            "x-ddc-secret",
            "x-admin-token"
    );

    private final ConnectionProvider connectionProvider;

    private final HttpClient client;

    public ReactorNettyHttpUpstreamAdapter(
            int maxConnections,
            int pendingAcquireMaxCount,
            Duration idleTimeout) {
        connectionProvider = ConnectionProvider.builder("gateway-http-upstream")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .maxIdleTime(idleTimeout)
                .build();
        client = HttpClient.create(connectionProvider)
                .compress(true);
    }

    @Override
    public Mono<GatewayOutboundHttpResponse> invoke(
            HttpUpstreamRequest request) {
        String scheme = request.provider().secure() ? "https" : "http";
        return client
                .headers(headers -> {
                    request.headers().forEach((name, values) -> {
                        if (!FORBIDDEN.contains(name.toLowerCase(Locale.ROOT))) {
                            values.forEach(value -> headers.add(name, value));
                        }
                    });
                    headers.set("host", request.provider().host()
                            + ":" + request.provider().port());
                })
                .request(HttpMethod.valueOf(request.method()))
                .uri(scheme
                        + "://"
                        + request.provider().host()
                        + ":"
                        + request.provider().port()
                        + request.pathAndQuery())
                .send((ignored, outbound) ->
                        outbound.sendByteArray(request.body()))
                .responseSingle((response, body) -> body
                        .asByteArray()
                        .defaultIfEmpty(new byte[0])
                        .map(bytes -> new GatewayOutboundHttpResponse(
                                response.status().code(),
                                responseHeaders(response.responseHeaders()),
                                reactor.core.publisher.Flux.just(bytes)
                        )))
                .timeout(request.timeout());
    }

    @Override
    public void close() {
        connectionProvider.disposeLater().block();
    }

    private Map<String, List<String>> responseHeaders(
            io.netty.handler.codec.http.HttpHeaders headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        headers.forEach(entry -> {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            if (!FORBIDDEN.contains(name)) {
                result.computeIfAbsent(
                        name,
                        ignored -> new ArrayList<>()
                ).add(entry.getValue());
            }
        });
        return Map.copyOf(result);
    }
}
