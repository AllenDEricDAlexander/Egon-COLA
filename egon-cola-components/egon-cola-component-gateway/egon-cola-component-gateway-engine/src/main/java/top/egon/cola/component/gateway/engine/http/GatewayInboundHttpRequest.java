package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayInboundHttpRequest(
        String method,
        String host,
        String uri,
        Map<String, List<String>> headers,
        InetSocketAddress remoteAddress,
        Flux<byte[]> body
) {

    public GatewayInboundHttpRequest {
        method = Objects.requireNonNull(method, "method");
        host = Objects.requireNonNull(host, "host");
        uri = Objects.requireNonNull(uri, "uri");
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
    }
}
