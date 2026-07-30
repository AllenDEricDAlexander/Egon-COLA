package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayOutboundHttpResponse(
        int status,
        Map<String, List<String>> headers,
        Flux<DataBuffer> body
) {

    public GatewayOutboundHttpResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("invalid HTTP status");
        }
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
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
