package top.egon.cola.component.gateway.engine.operation;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.rpc.HttpRpcUpstreamAdapter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts direct operation requests to the existing HTTP and HTTP-to-RPC
 * upstream adapters.
 */
public final class DefaultGatewayOperationTransport
        implements EngineGatewayOperationInvoker.OperationTransport {

    private static final DefaultDataBufferFactory BUFFER_FACTORY =
            DefaultDataBufferFactory.sharedInstance;

    private final HttpUpstreamAdapter http;

    private final HttpRpcUpstreamAdapter rpc;

    private final long maximumResponseBytes;

    public DefaultGatewayOperationTransport(
            HttpUpstreamAdapter http,
            HttpRpcUpstreamAdapter rpc,
            long maximumResponseBytes) {
        this.http = Objects.requireNonNull(http, "http");
        this.rpc = Objects.requireNonNull(rpc, "rpc");
        if (maximumResponseBytes <= 0) {
            throw new IllegalArgumentException(
                    "maximumResponseBytes must be positive"
            );
        }
        this.maximumResponseBytes = maximumResponseBytes;
    }

    @Override
    public Mono<GatewayInvocationResult> invoke(
            ProviderInstance provider,
            EngineGatewayOperationInvoker.PreparedRequest request,
            Duration timeout) {
        Mono<GatewayOutboundHttpResponse> response =
                provider.serviceKey().protocolType()
                        == ProviderProtocolType.RPC
                        ? rpc(provider, request, timeout)
                        : http(provider, request, timeout);
        return response.flatMap(this::aggregate);
    }

    private Mono<GatewayOutboundHttpResponse> http(
            ProviderInstance provider,
            EngineGatewayOperationInvoker.PreparedRequest request,
            Duration timeout) {
        return http.invoke(new HttpUpstreamRequest(
                provider,
                request.method(),
                request.pathAndQuery(),
                request.headers(),
                Flux.defer(() -> request.body().length == 0
                        ? Flux.empty()
                        : Flux.just(BUFFER_FACTORY.wrap(request.body()))),
                timeout,
                true
        ));
    }

    private Mono<GatewayOutboundHttpResponse> rpc(
            ProviderInstance provider,
            EngineGatewayOperationInvoker.PreparedRequest request,
            Duration timeout) {
        String rawPath = rawPath(request.pathAndQuery());
        String rawQuery = rawQuery(request.pathAndQuery());
        RuntimeHttpRoute route = syntheticRoute(request, provider, rawPath);
        return rpc.invoke(
                new HttpRouteMatch(route, request.pathVariables()),
                provider,
                new NormalizedHttpRequest(
                        request.method(),
                        "mcp.local",
                        rawPath,
                        rawPath,
                        rawQuery,
                        request.headers()
                ),
                request.body(),
                request.headers(),
                timeout
        );
    }

    private RuntimeHttpRoute syntheticRoute(
            EngineGatewayOperationInvoker.PreparedRequest request,
            ProviderInstance provider,
            String rawPath) {
        Map<String, String> metadata = new LinkedHashMap<>(
                request.operation().attributes()
        );
        metadata.put("methodIdentity", request.operation().methodIdentity());
        if (request.operation().requestSchema() != null) {
            metadata.put(
                    "requestSchema",
                    request.operation().requestSchema()
            );
        }
        if (request.operation().responseSchema() != null) {
            metadata.put(
                    "responseSchema",
                    request.operation().responseSchema()
            );
        }
        return new RuntimeHttpRoute(
                "mcp:" + request.operation().operationId(),
                request.operation().operationId(),
                "mcp-local",
                Set.of(AccessZone.INTERNAL),
                "*",
                Set.of(request.method()),
                rawPath,
                false,
                provider.serviceKey(),
                request.operation().policyRefs(),
                0,
                responseMode(request.operation().responseMode()),
                Map.copyOf(metadata),
                EffectiveGatewayTransportPolicy.legacy()
        );
    }

    private GatewayResponseMode responseMode(String value) {
        try {
            return GatewayResponseMode.valueOf(value.toUpperCase(
                    Locale.ROOT
            ));
        } catch (IllegalArgumentException ignored) {
            return GatewayResponseMode.TRANSPARENT;
        }
    }

    private Mono<GatewayInvocationResult> aggregate(
            GatewayOutboundHttpResponse response) {
        int limit = (int) Math.min(Integer.MAX_VALUE, maximumResponseBytes);
        Mono<byte[]> body = DataBufferUtils.join(response.body(), limit)
                .map(this::bytes)
                .switchIfEmpty(Mono.just(new byte[0]));
        return body.map(bytes -> new GatewayInvocationResult(
                response.status(),
                response.headers(),
                bytes
        ));
    }

    private byte[] bytes(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    private String rawPath(String pathAndQuery) {
        int query = pathAndQuery.indexOf('?');
        return query < 0 ? pathAndQuery : pathAndQuery.substring(0, query);
    }

    private String rawQuery(String pathAndQuery) {
        int query = pathAndQuery.indexOf('?');
        return query < 0 ? "" : pathAndQuery.substring(query + 1);
    }
}
