package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.channel.ChannelOption;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferPipeline;
import top.egon.cola.component.gateway.engine.transport.GatewayTransportTimeouts;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReactorNettyHttpUpstreamAdapter
        implements HttpUpstreamAdapter, AutoCloseable {

    private static final NettyDataBufferFactory BUFFER_FACTORY =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    private static final Set<String> FORBIDDEN = Set.of(
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

    private final GatewayHeaderFilter headerFilter =
            new GatewayHeaderFilter();

    public ReactorNettyHttpUpstreamAdapter(
            int maxConnections,
            int pendingAcquireMaxCount,
            Duration idleTimeout) {
        connectionProvider = ConnectionProvider.builder("gateway-http-upstream")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .maxIdleTime(idleTimeout)
                .build();
        client = HttpClient.create(connectionProvider);
    }

    @Override
    public Mono<GatewayOutboundHttpResponse> invoke(
            HttpUpstreamRequest request) {
        String scheme = request.provider().secure() ? "https" : "http";
        long startedNanos = System.nanoTime();
        Mono<GatewayOutboundHttpResponse> result = client
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        timeoutMillis(request.connectTimeout())
                )
                .responseTimeout(request.responseHeaderTimeout())
                .headers(headers -> {
                    headerFilter.requestHeaders(request.headers())
                            .forEach((name, values) -> {
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
                        outbound.send(
                                GatewayDataBufferPipeline
                                        .releaseOnDiscardOrCancel(
                                                GatewayTransportTimeouts
                                                        .requestIdle(
                                                                request.body(),
                                                                request.streamIdleTimeout()
                                                        )
                                        )
                                        .map(buffer ->
                                                GatewayDataBufferOwnership
                                                        .transferToNetty(
                                                                buffer,
                                                                outbound.alloc()
                                                        ))
                        ))
                .responseConnection((response, connection) -> {
                    AtomicBoolean disposed = new AtomicBoolean();
                    Runnable dispose = () -> {
                        if (disposed.compareAndSet(false, true)) {
                            connection.dispose();
                        }
                    };
                    Flux<DataBuffer> body = GatewayTransportTimeouts
                            .responseIdle(
                                    connection.inbound()
                                            .receive()
                                            .<DataBuffer>map(buffer ->
                                                    GatewayDataBufferOwnership
                                                            .retainAndWrap(
                                                                    BUFFER_FACTORY,
                                                                    buffer
                                                            )),
                                    request.streamIdleTimeout()
                            );
                    body = GatewayTransportTimeouts.total(
                            body,
                            remainingTotal(request, startedNanos)
                    )
                            .doFinally(ignored -> dispose.run());
                    return Flux.just(new GatewayOutboundHttpResponse(
                            response.status().code(),
                            responseHeaders(response.responseHeaders()),
                            body,
                            GatewayHttpFlushMode.STANDARD,
                            dispose
                    ));
                })
                .single();
        return GatewayTransportTimeouts.total(
                result,
                request.totalTimeout()
        );
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
        return headerFilter.responseHeaders(result);
    }

    private int timeoutMillis(Duration timeout) {
        return Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                Math.max(1, timeout.toMillis())
        ));
    }

    private java.util.Optional<Duration> remainingTotal(
            HttpUpstreamRequest request,
            long startedNanos) {
        return request.totalTimeout().map(total -> {
            long elapsed = Math.max(0, System.nanoTime() - startedNanos);
            long remaining = Math.max(1, total.toNanos() - elapsed);
            return Duration.ofNanos(remaining);
        });
    }
}
