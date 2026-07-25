package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallObservation;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.traffic.GatewayRequestResourceGuard;
import top.egon.cola.component.gateway.engine.traffic.GatewayResourceLimits;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultGatewayHttpDataPlaneHandler
        implements GatewayHttpDataPlaneHandler {

    private final HttpRequestNormalizer normalizer;

    private final Supplier<CompiledHttpRouteIndex> routeIndex;

    private final ProviderSelector providerSelector;

    private final HttpUpstreamAdapter upstreamAdapter;

    private final long maxBodyBytes;

    private final Duration upstreamTimeout;

    private final GatewayRequestResourceGuard resourceGuard;

    private final GatewayHttpSecurityProcessor securityProcessor;

    private final GatewayCallCompletionListener completionListener;

    private final String engineNodeId;

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
                        ),
                GatewayCallCompletionListener.noop(),
                "unknown-engine"
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
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                GatewayCallCompletionListener.noop(),
                "unknown-engine"
        );
    }

    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId) {
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
        this.completionListener = Objects.requireNonNull(
                completionListener,
                "completionListener"
        );
        this.engineNodeId = Objects.requireNonNull(
                engineNodeId,
                "engineNodeId"
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
        GatewayTraceContext trace = traceContext(request.headers());
        GatewayCallObservation observation = GatewayCallObservation.start(
                trace,
                "HTTP",
                accessZone.name(),
                engineNodeId
        );
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
                return Mono.just(observed(
                        error(
                                404,
                                "GATEWAY_ROUTE_NOT_FOUND",
                                trace.traceId()
                        ),
                        observation,
                        "ROUTE",
                        "REJECTED",
                        "GATEWAY_ROUTE_NOT_FOUND"
                ));
            }
            observation.route(
                    normalized.method(),
                    match.route().pathPattern(),
                    match.route().gatewayGroupId(),
                    match.route().metadata().get("releaseId"),
                    match.route().operationId(),
                    match.route().routeId()
            );
            observation.scope(
                    match.route().upstream().env(),
                    match.route().upstream().namespace()
            );
            if (accessZone == AccessZone.PUBLIC
                    && !match.route().externalAccessible()) {
                return Mono.just(observed(
                        error(
                                404,
                                "GATEWAY_ROUTE_NOT_FOUND",
                                trace.traceId()
                        ),
                        observation,
                        "EXPOSURE",
                        "REJECTED",
                        "GATEWAY_ROUTE_NOT_FOUND"
                ));
            }
            return securityProcessor.authorize(
                            accessZone,
                            request,
                            normalized,
                            match,
                            trace.traceId()
                    )
                    .flatMap(security -> {
                        observation.governance(
                                "NOT_APPLIED",
                                "NOT_APPLIED",
                                "ALLOW"
                        );
                        return Mono.using(
                            () -> providerSelector.select(
                                    match.route().upstream()
                            ),
                            selection -> invokeUpstream(
                                    selection.instance(),
                                    normalized,
                                    request,
                                    security,
                                    trace,
                                    observation
                            ),
                            ProviderSelectionHandle::close
                        );
                    })
                    .map(response -> observed(
                            response,
                            observation,
                            "COMPLETE",
                            category(response.status()),
                            response.status() >= 400
                                    ? "GATEWAY_UPSTREAM_STATUS"
                                    : null
                    ))
                    .onErrorResume(GatewaySecurityException.class,
                            rejected -> Mono.just(observed(
                                    error(
                                            rejected.httpStatus(),
                                            rejected.code(),
                                            trace.traceId()
                                    ),
                                    observation,
                                    "SECURITY",
                                    "REJECTED",
                                    rejected.code()
                            )))
                    .onErrorResume(GatewayRequestRejectedException.class,
                            rejected -> Mono.just(observed(
                                    error(
                                            rejected.status(),
                                            rejected.code(),
                                            trace.traceId()
                                    ),
                                    observation,
                                    "RESOURCE",
                                    "REJECTED",
                                    rejected.code()
                            )))
                    .onErrorResume(java.util.concurrent.TimeoutException.class,
                            timeout -> Mono.just(observed(
                                    error(
                                            504,
                                            "GATEWAY_UPSTREAM_TIMEOUT",
                                            trace.traceId()
                                    ),
                                    observation,
                                    "UPSTREAM",
                                    "TIMEOUT",
                                    "GATEWAY_UPSTREAM_TIMEOUT"
                            )))
                    .onErrorResume(error -> Mono.just(observed(
                            error(
                                    502,
                                    "GATEWAY_UPSTREAM_CONNECT_FAILED",
                                    trace.traceId()
                            ),
                            observation,
                            "UPSTREAM",
                            "ERROR",
                            "GATEWAY_UPSTREAM_CONNECT_FAILED"
                    )))
                    .doOnCancel(() -> publish(
                            observation,
                            "CLIENT",
                            "CANCELLED",
                            "GATEWAY_CLIENT_CANCELLED",
                            null
                    ));
        } catch (GatewayRequestRejectedException rejected) {
            return Mono.just(observed(
                    error(
                            rejected.status(),
                            rejected.code(),
                            trace.traceId()
                    ),
                    observation,
                    "NORMALIZE",
                    "REJECTED",
                    rejected.code()
            ));
        } catch (RuntimeException error) {
            return Mono.just(observed(
                    error(
                            500,
                            "GATEWAY_INTERNAL_ERROR",
                            trace.traceId()
                    ),
                    observation,
                    "INTERNAL",
                    "ERROR",
                    "GATEWAY_INTERNAL_ERROR"
            ));
        }
    }

    private Mono<GatewayOutboundHttpResponse> invokeUpstream(
            ProviderInstance provider,
            NormalizedHttpRequest normalized,
            GatewayInboundHttpRequest request,
            GatewayHttpSecurityProcessor.Outcome security,
            GatewayTraceContext trace,
            GatewayCallObservation observation) {
        long attemptStartedAt = System.currentTimeMillis();
        long attemptStartedNanos = System.nanoTime();
        String attemptSpanId = trace.newChildSpanId();
        observation.provider(provider.instanceId(), Map.of(
                "serviceKey",
                provider.serviceKey().serviceName(),
                "protocol",
                provider.serviceKey().protocolType().name(),
                "version",
                provider.serviceKey().version(),
                "group",
                provider.serviceKey().group()
        ));
        return aggregate(request.body())
                .doOnNext(body -> observation.addRequestBytes(body.length))
                .flatMap(body -> upstreamAdapter.invoke(new HttpUpstreamRequest(
                        provider,
                        normalized.method(),
                        normalized.normalizedPath()
                                + (normalized.rawQuery().isEmpty()
                                ? ""
                                : "?" + normalized.rawQuery()),
                        forwardedHeaders(
                                normalized.headers(),
                                trace,
                                attemptSpanId,
                                security
                        ),
                        reactor.core.publisher.Flux.just(body),
                        upstreamTimeout
                )))
                .doOnSuccess(response -> observation.attempt(
                        1,
                        attemptSpanId,
                        provider.instanceId(),
                        attemptStartedAt,
                        elapsedMillis(attemptStartedNanos),
                        category(response.status()),
                        null
                ))
                .doOnError(failure -> observation.attempt(
                        1,
                        attemptSpanId,
                        provider.instanceId(),
                        attemptStartedAt,
                        elapsedMillis(attemptStartedNanos),
                        "ERROR",
                        null
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
            GatewayTraceContext trace,
            String childSpanId,
            GatewayHttpSecurityProcessor.Outcome security) {
        Map<String, List<String>> sanitized =
                identitySanitizer.sanitizeHttp(
                source,
                security.fieldsToRemove(),
                security.trustedIdentity(),
                trace.traceId()
        );
        Map<String, List<String>> result = new LinkedHashMap<>(sanitized);
        result.put(
                "traceparent",
                List.of(trace.childTraceparent(childSpanId))
        );
        if (trace.tracestate() != null) {
            result.put("tracestate", List.of(trace.tracestate()));
        }
        return Map.copyOf(result);
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

    private GatewayOutboundHttpResponse observed(
            GatewayOutboundHttpResponse response,
            GatewayCallObservation observation,
            String terminalStage,
            String category,
            String code) {
        Map<String, List<String>> headers = new LinkedHashMap<>(
                response.headers()
        );
        headers.put(
                "x-trace-id",
                List.of(observation.trace().traceId())
        );
        return new GatewayOutboundHttpResponse(
                response.status(),
                headers,
                response.body()
                        .doOnNext(bytes -> observation.addResponseBytes(
                                bytes.length
                        ))
                        .doOnComplete(() -> publish(
                                observation,
                                terminalStage,
                                category,
                                code,
                                response.status()
                        ))
                        .doOnError(failure -> publish(
                                observation,
                                "RESPONSE",
                                "ERROR",
                                "GATEWAY_RESPONSE_STREAM_ERROR",
                                response.status()
                        ))
                        .doOnCancel(() -> publish(
                                observation,
                                "RESPONSE",
                                "CANCELLED",
                                "GATEWAY_CLIENT_CANCELLED",
                                response.status()
                        ))
        );
    }

    private void publish(
            GatewayCallObservation observation,
            String stage,
            String category,
            String code,
            Integer status) {
        observation.complete(stage, category, code, status, null)
                .ifPresent(completionListener::onComplete);
    }

    private GatewayTraceContext traceContext(
            Map<String, List<String>> headers) {
        return GatewayTraceContext.select(
                firstHeader(headers, "traceparent"),
                firstHeader(headers, "x-trace-id"),
                firstHeader(headers, "tracestate")
        );
    }

    private String firstHeader(
            Map<String, List<String>> headers,
            String expected) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(expected))
                .map(Map.Entry::getValue)
                .filter(values -> values != null && !values.isEmpty())
                .map(List::getFirst)
                .findFirst()
                .orElse(null);
    }

    private String category(int status) {
        if (status < 400) {
            return "SUCCESS";
        }
        return status < 500 ? "REJECTED" : "ERROR";
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(
                0,
                (System.nanoTime() - startedNanos) / 1_000_000
        );
    }
}
