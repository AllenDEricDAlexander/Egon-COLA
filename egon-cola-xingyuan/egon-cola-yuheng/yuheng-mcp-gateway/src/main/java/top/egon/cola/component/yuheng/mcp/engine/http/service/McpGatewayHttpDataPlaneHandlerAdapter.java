package top.egon.cola.component.yuheng.mcp.engine.http.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.mcp.common.transport.McpHttpRequest;
import top.egon.cola.component.yuheng.mcp.common.transport.McpHttpResponse;
import top.egon.cola.component.yuheng.mcp.engine.config.McpGatewayEngineProperties;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpEngineHttpHandler;
import top.egon.cola.component.yuheng.runtime.http.domain.GatewayHttpFlushMode;
import top.egon.cola.component.yuheng.runtime.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.yuheng.runtime.http.service.GatewayHttpDataPlaneHandler;
import top.egon.cola.component.yuheng.runtime.http.service.GatewayOutboundHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 中文说明：只适配 MCP HTTP，不为普通 API 请求提供回退路由。
 * English summary: Preserves MCP wire responses and SSE flushing on the shared HTTP listener.
 * 用法 / Usage: Construct with validated listener limits and the role-local MCP handler.
 */
@Slf4j
@RequiredArgsConstructor
public final class McpGatewayHttpDataPlaneHandlerAdapter implements GatewayHttpDataPlaneHandler {

    @NonNull
    @Qualifier("gatewayMcpHttpHandler")
    private final McpEngineHttpHandler handler;
    @NonNull
    @Qualifier("mcpGatewayListenerProperties")
    private final McpGatewayEngineProperties.ListenerProperties properties;

    @Override
    public Mono<GatewayOutboundHttpResponse> handle(AccessZone zone, GatewayInboundHttpRequest request) {
        if (!handler.supports(request.uri())) {
            return request.body().doOnNext(DataBufferUtils::release).then(
                    Mono.just(GatewayOutboundHttpResponse.text(404, "MCP_ROUTE_NOT_FOUND")));
        }
        return DataBufferUtils.join(request.body(), Math.toIntExact(properties.maximumRequestBytes()))
                .map(buffer -> {
                    try {
                        return buffer.toString(StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .defaultIfEmpty("")
                .flatMap(body -> {
                    Map<String, String> headers = new LinkedHashMap<>();
                    request.headers().forEach((name, values) -> {
                        if (!values.isEmpty()) {
                            headers.put(name, values.getFirst());
                        }
                    });
                    String remote = request.remoteAddress() == null ? "unknown"
                            : request.remoteAddress().getAddress().getHostAddress();
                    return handler.handle(new McpHttpRequest(request.method(), request.uri(),
                            Map.copyOf(headers), body,
                            Map.of("accessZone", zone.name(), "remoteAddress", remote)));
                })
                .map(this::adapt)
                .onErrorResume(DataBufferLimitException.class, ignored -> Mono.just(
                        new GatewayOutboundHttpResponse(413,
                                Map.of("content-type", java.util.List.of("application/json; charset=UTF-8")),
                                Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(
                                        "{\"error\":\"MCP_BODY_TOO_LARGE\"}".getBytes(StandardCharsets.UTF_8))))));
    }

    private GatewayOutboundHttpResponse adapt(McpHttpResponse response) {
        var result = new GatewayOutboundHttpResponse(response.status(), response.headers(),
                Flux.from(response.body()).map(DefaultDataBufferFactory.sharedInstance::wrap));
        return response.flushPerEvent() ? result.withFlushMode(GatewayHttpFlushMode.PER_BUFFER) : result;
    }
}
