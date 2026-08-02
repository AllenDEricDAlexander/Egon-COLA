package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.engine.mcp.McpEngineHttpHandler;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.mcp.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.transport.McpHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Front controller that reserves fixed platform paths before rule routing.
 */
public final class GatewayCompositeHttpDataPlaneHandler
        implements GatewayHttpDataPlaneHandler {

    private static final int DEFAULT_MCP_MAX_BODY_BYTES = 2 * 1024 * 1024;

    private final McpEngineHttpHandler mcp;

    private final GatewayHttpDataPlaneHandler routes;

    private final int maximumMcpBodyBytes;

    public GatewayCompositeHttpDataPlaneHandler(
            McpEngineHttpHandler mcp,
            GatewayHttpDataPlaneHandler routes) {
        this(mcp, routes, DEFAULT_MCP_MAX_BODY_BYTES);
    }

    public GatewayCompositeHttpDataPlaneHandler(
            McpEngineHttpHandler mcp,
            GatewayHttpDataPlaneHandler routes,
            int maximumMcpBodyBytes) {
        this.mcp = Objects.requireNonNull(mcp, "mcp");
        this.routes = Objects.requireNonNull(routes, "routes");
        if (maximumMcpBodyBytes < 1) {
            throw new IllegalArgumentException(
                    "maximumMcpBodyBytes must be positive"
            );
        }
        this.maximumMcpBodyBytes = maximumMcpBodyBytes;
    }

    @Override
    public Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        if (!mcp.supports(request.uri())) {
            return routes.handle(accessZone, request);
        }
        return aggregate(request)
                .flatMap(body -> mcp.handle(new McpHttpRequest(
                        request.method(),
                        request.uri(),
                        firstHeaders(request.headers()),
                        body,
                        Map.of(
                                "accessZone", accessZone.name(),
                                "remoteAddress", remoteAddress(request)
                        )
                )))
                .map(this::adapt)
                .onErrorResume(
                        DataBufferLimitException.class,
                        ignored -> Mono.just(new GatewayOutboundHttpResponse(
                                413,
                                Map.of("content-type", List.of(
                                        "application/json; charset=UTF-8"
                                )),
                                Flux.just(DefaultDataBufferFactory
                                        .sharedInstance.wrap(
                                                ("{\"error\":"
                                                        + "\"MCP_BODY_TOO_LARGE"
                                                        + "\"}").getBytes(
                                                        StandardCharsets.UTF_8
                                                )
                                        ))
                        ))
                );
    }

    @Override
    public Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        return routes.prepareWebSocket(accessZone, request);
    }

    @Override
    public Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        return routes.bridgeWebSocket(upstream, downstream);
    }

    private Mono<String> aggregate(GatewayInboundHttpRequest request) {
        return DataBufferUtils.join(request.body(), maximumMcpBodyBytes)
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .defaultIfEmpty("");
    }

    private GatewayOutboundHttpResponse adapt(McpHttpResponse response) {
        Flux<DataBuffer> body = Flux.from(response.body())
                .map(bytes -> DefaultDataBufferFactory.sharedInstance.wrap(
                        bytes
                ));
        GatewayOutboundHttpResponse adapted =
                new GatewayOutboundHttpResponse(
                        response.status(),
                        response.headers(),
                        body
                );
        return response.flushPerEvent()
                ? adapted.withFlushMode(GatewayHttpFlushMode.PER_BUFFER)
                : adapted;
    }

    private Map<String, String> firstHeaders(
            Map<String, List<String>> headers) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                result.put(name, values.getFirst());
            }
        });
        return Map.copyOf(result);
    }

    private String remoteAddress(GatewayInboundHttpRequest request) {
        return request.remoteAddress() == null
                ? "unknown"
                : request.remoteAddress().getAddress().getHostAddress();
    }
}
