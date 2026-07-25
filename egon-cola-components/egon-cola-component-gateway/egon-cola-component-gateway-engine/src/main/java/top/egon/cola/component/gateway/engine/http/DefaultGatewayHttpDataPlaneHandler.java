package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.common.id.uuid.UuidV7;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.traffic.GatewayRequestResourceGuard;
import top.egon.cola.component.gateway.engine.traffic.GatewayResourceLimits;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class DefaultGatewayHttpDataPlaneHandler
        implements GatewayHttpDataPlaneHandler {

    private static final Pattern TRACE_ID =
            Pattern.compile("[A-Za-z0-9_-]{16,64}");

    private final HttpRequestNormalizer normalizer;

    private final Supplier<CompiledHttpRouteIndex> routeIndex;

    private final ProviderSelector providerSelector;

    private final HttpUpstreamAdapter upstreamAdapter;

    private final long maxBodyBytes;

    private final Duration upstreamTimeout;

    private final GatewayRequestResourceGuard resourceGuard;

    private final GatewayHttpSecurityProcessor securityProcessor;

    private final TrustedIdentitySanitizer identitySanitizer =
            new TrustedIdentitySanitizer();

    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                (zone, request, normalized, route, traceId) ->
                        Mono.just(
                                GatewayHttpSecurityProcessor.Outcome.anonymous()
                        )
        );
    }

    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor) {
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
        this.routeIndex = Objects.requireNonNull(routeIndex, "routeIndex");
        this.providerSelector = Objects.requireNonNull(
                providerSelector,
                "providerSelector"
        );
        this.upstreamAdapter = Objects.requireNonNull(
                upstreamAdapter,
                "upstreamAdapter"
        );
        this.maxBodyBytes = maxBodyBytes;
        this.upstreamTimeout = Objects.requireNonNull(
                upstreamTimeout,
                "upstreamTimeout"
        );
        this.securityProcessor = Objects.requireNonNull(
                securityProcessor,
                "securityProcessor"
        );
        resourceGuard = new GatewayRequestResourceGuard(
                new GatewayResourceLimits(
                        128,
                        64,
                        64 * 1024,
                        maxBodyBytes,
                        4 * 1024 * 1024
                )
        );
    }

    @Override
    public Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        try {
            NormalizedHttpRequest normalized = normalizer.normalize(
                    request.method(),
                    request.host(),
                    request.uri(),
                    request.headers()
            );
            resourceGuard.validate(normalized);
            HttpRouteMatch match = routeIndex.get().match(
                    normalized.host(),
                    normalized.method(),
                    normalized.normalizedPath(),
                    accessZone
            ).orElse(null);
            if (match == null) {
                return Mono.just(error(404, "GATEWAY_ROUTE_NOT_FOUND", traceId(
                        normalized.headers()
                )));
            }
            if (accessZone == AccessZone.PUBLIC
                    && !match.route().externalAccessible()) {
                return Mono.just(error(
                        404,
                        "GATEWAY_ROUTE_NOT_FOUND",
                        traceId(normalized.headers())
                ));
            }
            String traceId = traceId(normalized.headers());
            return securityProcessor.authorize(
                            accessZone,
                            request,
                            normalized,
                            match,
                            traceId
                    )
                    .flatMap(security -> Mono.using(
                            () -> providerSelector.select(
                                    match.route().upstream()
                            ),
                            selection -> invokeUpstream(
                                    selection.instance(),
                                    normalized,
                                    request,
                                    security
                            ),
                            ProviderSelectionHandle::close
                    ))
                    .onErrorResume(GatewaySecurityException.class,
                            rejected -> Mono.just(error(
                                    rejected.httpStatus(),
                                    rejected.code(),
                                    traceId
                            )))
                    .onErrorResume(GatewayRequestRejectedException.class,
                            rejected -> Mono.just(error(
                                    rejected.status(),
                                    rejected.code(),
                                    traceId(normalized.headers())
                            )))
                    .onErrorResume(java.util.concurrent.TimeoutException.class,
                            timeout -> Mono.just(error(
                                    504,
                                    "GATEWAY_UPSTREAM_TIMEOUT",
                                    traceId(normalized.headers())
                            )))
                    .onErrorResume(error -> Mono.just(error(
                            502,
                            "GATEWAY_UPSTREAM_CONNECT_FAILED",
                            traceId(normalized.headers())
                    )));
        } catch (GatewayRequestRejectedException rejected) {
            return Mono.just(error(
                    rejected.status(),
                    rejected.code(),
                    UuidV7.simpleString()
            ));
        } catch (RuntimeException error) {
            return Mono.just(error(
                    500,
                    "GATEWAY_INTERNAL_ERROR",
                    UuidV7.simpleString()
            ));
        }
    }

    private Mono<GatewayOutboundHttpResponse> invokeUpstream(
            ProviderInstance provider,
            NormalizedHttpRequest normalized,
            GatewayInboundHttpRequest request,
            GatewayHttpSecurityProcessor.Outcome security) {
        return aggregate(request.body())
                .flatMap(body -> upstreamAdapter.invoke(
                        new HttpUpstreamRequest(
                                provider,
                                normalized.method(),
                                normalized.normalizedPath()
                                        + (normalized.rawQuery().isEmpty()
                                        ? ""
                                        : "?" + normalized.rawQuery()),
                                forwardedHeaders(
                                        normalized.headers(),
                                        traceId(normalized.headers()),
                                        security
                                ),
                                reactor.core.publisher.Flux.just(body),
                                upstreamTimeout
                        )
                ));
    }

    private Mono<byte[]> aggregate(reactor.core.publisher.Flux<byte[]> body) {
        return body.collect(
                ByteArrayOutputStream::new,
                (output, bytes) -> {
                    if ((long) output.size() + bytes.length > maxBodyBytes) {
                        throw new GatewayRequestRejectedException(
                                "GATEWAY_REQUEST_BODY_TOO_LARGE",
                                413,
                                "request body exceeds configured limit"
                        );
                    }
                    output.writeBytes(bytes);
                }
        ).map(ByteArrayOutputStream::toByteArray);
    }

    private Map<String, List<String>> forwardedHeaders(
            Map<String, List<String>> source,
            String traceId,
            GatewayHttpSecurityProcessor.Outcome security) {
        return identitySanitizer.sanitizeHttp(
                source,
                security.fieldsToRemove(),
                security.trustedIdentity(),
                traceId
        );
    }

    private String traceId(Map<String, List<String>> headers) {
        List<String> values = headers.getOrDefault("x-trace-id", List.of());
        if (!values.isEmpty() && TRACE_ID.matcher(values.getFirst()).matches()) {
            return values.getFirst();
        }
        return UuidV7.simpleString();
    }

    private GatewayOutboundHttpResponse error(
            int status,
            String code,
            String traceId) {
        String body = "{\"success\":false,\"code\":\""
                + code
                + "\",\"traceId\":\""
                + traceId
                + "\"}";
        return new GatewayOutboundHttpResponse(
                status,
                Map.of(
                        "content-type",
                        List.of("application/json; charset=UTF-8"),
                        "x-trace-id",
                        List.of(traceId)
                ),
                reactor.core.publisher.Flux.just(
                        body.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                )
        );
    }
}
