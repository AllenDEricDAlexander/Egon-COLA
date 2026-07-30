package top.egon.cola.component.gateway.engine.http;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.error.GatewayError;
import top.egon.cola.component.gateway.contract.error.GatewayErrorCategory;
import top.egon.cola.component.gateway.contract.error.GatewayResult;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayHeaders;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;

import java.util.List;
import java.util.Map;

public abstract class AbstractGatewayHttpStageExchange
        implements GatewayExchange {

    private final GatewayInboundHttpRequest inbound;

    private final GatewayContext context;

    private final GatewayRequest request;

    private GatewayResponse response;

    private GatewayWebSocketHandshakeResult webSocketResult;

    protected AbstractGatewayHttpStageExchange(
            GatewayInboundHttpRequest inbound,
            GatewayContext context) {
        this.inbound = inbound;
        this.context = context;
        request = request(inbound, context);
    }

    public abstract Publisher<GatewayResponse> cors(
            GatewayFilterChain chain);

    public abstract Publisher<GatewayResponse> security(
            GatewayFilterChain chain);

    public abstract Publisher<GatewayResponse> governance(
            GatewayFilterChain chain);

    public abstract Publisher<GatewayResponse> invoke();

    public abstract GatewayOutboundHttpResponse mapFailure(
            Throwable failure);

    protected final Publisher<GatewayResponse> respond(
            GatewayOutboundHttpResponse outbound) {
        response = new GatewayHttpBridgeResponse(
                outbound,
                request.traceId()
        );
        return Mono.just(response);
    }

    protected final Publisher<GatewayResponse> respondWebSocket(
            GatewayWebSocketHandshakeResult result) {
        webSocketResult = result;
        response = new GatewayWebSocketBridgeResponse(
                result,
                request.traceId()
        );
        return Mono.just(response);
    }

    final GatewayResponse fail(Throwable failure) {
        response = new GatewayHttpBridgeResponse(
                mapFailure(failure),
                request.traceId()
        );
        return response;
    }

    final GatewayOutboundHttpResponse outbound() {
        if (!(response instanceof GatewayHttpBridgeResponse bridge)) {
            throw new IllegalStateException(
                    "gateway HTTP pipeline produced no response"
            );
        }
        return bridge.outbound();
    }

    final GatewayWebSocketHandshakeResult webSocketResult() {
        if (webSocketResult != null) {
            return webSocketResult;
        }
        if (response instanceof GatewayHttpBridgeResponse bridge) {
            return GatewayWebSocketHandshakeResult.rejected(
                    bridge.outbound().status(),
                    "GATEWAY_WEBSOCKET_REQUEST_REJECTED",
                    "gateway WebSocket request rejected before handshake"
            );
        }
        throw new IllegalStateException(
                "gateway WebSocket pipeline produced no handshake result"
        );
    }

    @Override
    public final GatewayRequest request() {
        return request;
    }

    @Override
    public final GatewayContext context() {
        return context;
    }

    @Override
    public final GatewayResponse response() {
        if (response == null) {
            return new GatewayHttpBridgeResponse(
                    GatewayOutboundHttpResponse.text(
                            500,
                            "gateway pipeline is incomplete"
                    ),
                    request.traceId()
            );
        }
        return response;
    }

    protected final GatewayInboundHttpRequest inbound() {
        return inbound;
    }

    private GatewayRequest request(
            GatewayInboundHttpRequest source,
            GatewayContext gatewayContext) {
        GatewayTraceContext selected = gatewayContext == null
                ? GatewayTraceContext.fromHeaders(
                header(source.headers(), "traceparent", null),
                header(source.headers(), "tracestate", null),
                header(source.headers(), "x-egon-request-id", null)
        )
                : null;
        String traceId = gatewayContext == null
                ? selected.traceId()
                : gatewayContext.traceId();
        String requestId = gatewayContext == null
                ? selected.requestId()
                : gatewayContext.requestId();
        AccessZone accessZone = gatewayContext == null
                ? AccessZone.INTERNAL
                : gatewayContext.accessZone();
        ImmutableGatewayHeaders headers = new ImmutableGatewayHeaders(
                source.headers()
        );
        return new GatewayRequest() {
            @Override
            public String requestId() {
                return requestId;
            }

            @Override
            public String traceId() {
                return traceId;
            }

            @Override
            public GatewayProtocol protocol() {
                return GatewayProtocol.HTTP;
            }

            @Override
            public AccessZone accessZone() {
                return accessZone;
            }

            @Override
            public GatewayHeaders headers() {
                return headers;
            }

            @Override
            public GatewayBody body() {
                return new GatewayBody() {
                    @Override
                    public long contentLength() {
                        return AbstractGatewayHttpStageExchange.this
                                .contentLength(source.headers());
                    }

                    @Override
                    public boolean replayable() {
                        return false;
                    }
                };
            }
        };
    }

    private long contentLength(Map<String, List<String>> headers) {
        String raw = header(headers, "content-length", null);
        if (raw == null) {
            return -1;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String header(
            Map<String, List<String>> headers,
            String name,
            String defaultValue) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(defaultValue);
    }

    private record GatewayHttpBridgeResponse(
            GatewayOutboundHttpResponse outbound,
            String traceId
    ) implements GatewayResponse {

        @Override
        public GatewayResult result() {
            if (outbound.status() < 400) {
                return GatewayResult.success();
            }
            return GatewayResult.failure(new GatewayError(
                    "GATEWAY_HTTP_" + outbound.status(),
                    GatewayErrorCategory.UPSTREAM_FAILURE,
                    "Gateway HTTP request failed",
                    traceId,
                    outbound.status() >= 500,
                    Map.of()
            ));
        }

        @Override
        public GatewayHeaders headers() {
            return new ImmutableGatewayHeaders(outbound.headers());
        }

        @Override
        public GatewayBody body() {
            return EmptyGatewayBody.INSTANCE;
        }
    }

    private record GatewayWebSocketBridgeResponse(
            GatewayWebSocketHandshakeResult handshake,
            String traceId
    ) implements GatewayResponse {

        @Override
        public GatewayResult result() {
            if (handshake instanceof GatewayWebSocketHandshakeResult.Accepted) {
                return GatewayResult.success();
            }
            GatewayWebSocketHandshakeResult.Rejected rejected =
                    (GatewayWebSocketHandshakeResult.Rejected) handshake;
            return GatewayResult.failure(new GatewayError(
                    rejected.errorCode(),
                    GatewayErrorCategory.UPSTREAM_FAILURE,
                    rejected.message(),
                    traceId,
                    rejected.httpStatus() >= 500,
                    Map.of()
            ));
        }

        @Override
        public GatewayHeaders headers() {
            return new ImmutableGatewayHeaders(Map.of());
        }

        @Override
        public GatewayBody body() {
            return EmptyGatewayBody.INSTANCE;
        }
    }
}
