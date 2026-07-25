package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcome;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcomeRecorder;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallObservation;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.rpc.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.engine.traffic.GatewayRequestResourceGuard;
import top.egon.cola.component.gateway.engine.traffic.GatewayResourceLimits;
import top.egon.cola.component.gateway.engine.traffic.GatewayAttemptExecutor;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficRejectedException;
import top.egon.cola.component.gateway.engine.traffic.ProviderCallClassification;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class DefaultGatewayHttpDataPlaneHandler
        implements GatewayHttpDataPlaneHandler {

    private final HttpRequestNormalizer normalizer;

    private final Supplier<CompiledHttpRouteIndex> routeIndex;

    private final ProviderSelector providerSelector;

    private final HttpUpstreamAdapter upstreamAdapter;

    private final long maxBodyBytes;

    private final long maxResponseBytes = 4 * 1024 * 1024;

    private final Duration upstreamTimeout;

    private final GatewayRequestResourceGuard resourceGuard;

    private final GatewayHttpSecurityProcessor securityProcessor;

    private final GatewayCallCompletionListener completionListener;

    private final GatewayTrafficGovernance trafficGovernance;

    private final HttpRpcUpstreamAdapter httpRpcUpstream;

    private final ProviderCallOutcomeRecorder outcomeRecorder;

    private final GatewayAttemptExecutor attemptExecutor =
            new GatewayAttemptExecutor();

    private final String engineNodeId;

    private final TrustedIdentitySanitizer identitySanitizer =
            new TrustedIdentitySanitizer();

    private final GatewayBodySizeLimiter bodySizeLimiter =
            new GatewayBodySizeLimiter();

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
                "unknown-engine",
                GatewayTrafficGovernance.noop()
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
                "unknown-engine",
                GatewayTrafficGovernance.noop()
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
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                GatewayTrafficGovernance.noop(),
                null,
                ProviderCallOutcomeRecorder.noop()
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
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                null,
                ProviderCallOutcomeRecorder.noop()
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
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                ProviderCallOutcomeRecorder.noop()
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
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder) {
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
        this.trafficGovernance = Objects.requireNonNull(
                trafficGovernance,
                "trafficGovernance"
        );
        this.httpRpcUpstream = httpRpcUpstream;
        this.outcomeRecorder = Objects.requireNonNull(
                outcomeRecorder,
                "outcomeRecorder"
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
                        GatewayTrafficContext trafficContext =
                                trafficContext(
                                        match,
                                        normalized,
                                        request,
                                        security
                                );
                        return trafficGovernance.acquire(
                                        match.route().policyRefs(),
                                        trafficContext,
                                        upstreamTimeout
                                )
                                .flatMap(permit -> invokeUpstream(
                                                match,
                                                normalized,
                                                request,
                                                security,
                                                trace,
                                                observation,
                                                permit
                                        )
                                        .doFinally(signal ->
                                                permit.close()));
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
                    .onErrorResume(GatewayRequestBodyTooLargeException.class,
                            rejected -> Mono.just(observed(
                                    error(
                                            413,
                                            rejected.code(),
                                            trace.traceId()
                                    ),
                                    observation,
                                    "RESOURCE",
                                    "REJECTED",
                                    rejected.code()
                            )))
                    .onErrorResume(GatewayResponseBodyTooLargeException.class,
                            rejected -> Mono.just(observed(
                                    error(
                                            502,
                                            rejected.code(),
                                            trace.traceId()
                                    ),
                                    observation,
                                    "UPSTREAM",
                                    "REJECTED",
                                    rejected.code()
                            )))
                    .onErrorResume(GatewayTrafficRejectedException.class,
                            rejected -> {
                                observation.governance(
                                        "APPLIED",
                                        rejected.code(),
                                        "REJECT"
                                );
                                return Mono.just(observed(
                                        trafficError(
                                                rejected,
                                                trace.traceId()
                                        ),
                                        observation,
                                        "GOVERNANCE",
                                        "REJECTED",
                                        rejected.code()
                                ));
                            })
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
            HttpRouteMatch match,
            NormalizedHttpRequest normalized,
            GatewayInboundHttpRequest request,
            GatewayHttpSecurityProcessor.Outcome security,
            GatewayTraceContext trace,
            GatewayCallObservation observation,
            GatewayTrafficGovernance.RequestPermit permit) {
        long requestLimit = permit.requestSizeLimit(maxBodyBytes);
        bodySizeLimiter.validateRequestHeaders(request.headers(), requestLimit);
        return bodySizeLimiter.aggregateRequest(request.body(), requestLimit)
                .doOnNext(body -> observation.addRequestBytes(body.length))
                .flatMap(body -> {
                    AtomicInteger attempts = new AtomicInteger();
                    Set<String> failedProviders = new LinkedHashSet<>();
                    observation.governance(
                            "APPLIED",
                            permit.retryPolicy().enabled()
                                    ? "RETRY_ENABLED"
                                    : "RETRY_DISABLED",
                            "ALLOW"
                    );
                    return attemptExecutor.execute(
                            permit.retryPolicy(),
                            idempotent(match, normalized),
                            true,
                            permit.timeout(),
                            () -> invokeAttempt(
                                    match,
                                    normalized,
                                    body,
                                    security,
                                    trace,
                                    observation,
                                    permit,
                                    attempts.incrementAndGet(),
                                    failedProviders
                            ),
                            this::retryable
                    ).onErrorResume(
                            RetryableHttpStatusException.class,
                            failure -> Mono.just(failure.response())
                    );
                });
    }

    private Mono<GatewayOutboundHttpResponse> invokeAttempt(
            HttpRouteMatch match,
            NormalizedHttpRequest normalized,
            byte[] body,
            GatewayHttpSecurityProcessor.Outcome security,
            GatewayTraceContext trace,
            GatewayCallObservation observation,
            GatewayTrafficGovernance.RequestPermit requestPermit,
            int attemptNumber,
            Set<String> failedProviders) {
        ProviderSelectionHandle selection = providerSelector.select(
                match.route().upstream(),
                match.route().policyRefs(),
                failedProviders
        );
        ProviderInstance provider = selection.instance();
        GatewayTrafficGovernance.AttemptPermit attemptPermit;
        try {
            attemptPermit = requestPermit.acquireAttempt(provider);
        } catch (RuntimeException failure) {
            selection.close();
            return Mono.error(failure);
        }
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
        Map<String, List<String>> headers = forwardedHeaders(
                normalized.headers(),
                trace,
                attemptSpanId,
                security
        );
        Mono<GatewayOutboundHttpResponse> invocation;
        if (provider.serviceKey().protocolType()
                == ProviderProtocolType.RPC) {
            if (httpRpcUpstream == null) {
                invocation = Mono.error(new IllegalStateException(
                        "GATEWAY_HTTP_RPC_BRIDGE_UNAVAILABLE"
                ));
            } else {
                invocation = httpRpcUpstream.invoke(
                                match,
                                provider,
                                normalized,
                                body,
                                headers,
                                requestPermit.timeout()
                        )
                        .onErrorResume(
                                HttpRpcUpstreamAdapter
                                        .HttpRpcUpstreamException.class,
                                failure -> Mono.just(rpcError(
                                        failure,
                                        trace.traceId()
                                ))
                        );
            }
        } else {
            invocation = upstreamAdapter.invoke(new HttpUpstreamRequest(
                    provider,
                    normalized.method(),
                    normalized.normalizedPath()
                            + (normalized.rawQuery().isEmpty()
                            ? ""
                            : "?" + normalized.rawQuery()),
                    headers,
                    reactor.core.publisher.Flux.just(body),
                    requestPermit.timeout()
            ));
        }
        return invocation
                .map(response -> bodySizeLimiter.limitResponse(
                        response,
                        requestPermit.responseSizeLimit(maxResponseBytes)
                ))
                .doOnSuccess(response -> {
                    ProviderCallClassification classification =
                            classification(response.status());
                    attemptPermit.complete(classification);
                    outcomeRecorder.record(
                            provider.runtimeIdentity(),
                            healthOutcome(classification)
                    );
                    if (requestPermit.retryPolicy()
                            .retryableHttpStatus(response.status())) {
                        failedProviders.add(provider.runtimeIdentity());
                    }
                    observation.attempt(
                            attemptNumber,
                            attemptSpanId,
                            provider.instanceId(),
                            attemptStartedAt,
                            elapsedMillis(attemptStartedNanos),
                            category(response.status()),
                            null
                    );
                })
                .doOnError(failure -> {
                    attemptPermit.complete(
                            ProviderCallClassification.RETRYABLE_FAILURE
                    );
                    outcomeRecorder.record(
                            provider.runtimeIdentity(),
                            ProviderCallOutcome.RETRYABLE_FAILURE
                    );
                    failedProviders.add(provider.runtimeIdentity());
                    observation.attempt(
                            attemptNumber,
                            attemptSpanId,
                            provider.instanceId(),
                            attemptStartedAt,
                            elapsedMillis(attemptStartedNanos),
                            "ERROR",
                            retryable(failure)
                                    ? "RETRYABLE_UPSTREAM_FAILURE"
                                    : null
                    );
                })
                .doFinally(signal -> {
                    attemptPermit.close();
                    selection.close();
                })
                .flatMap(response -> requestPermit.retryPolicy()
                        .retryableHttpStatus(response.status())
                        ? Mono.error(new RetryableHttpStatusException(
                        response
                ))
                        : Mono.just(response));
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

    private GatewayOutboundHttpResponse trafficError(
            GatewayTrafficRejectedException rejected,
            String traceId) {
        GatewayOutboundHttpResponse response = error(
                rejected.httpStatus(),
                rejected.code(),
                traceId
        );
        if (rejected.retryAfterMillis() == 0) {
            return response;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>(
                response.headers()
        );
        headers.put(
                "retry-after",
                List.of(Long.toString(Math.max(
                        1,
                        (rejected.retryAfterMillis() + 999) / 1000
                )))
        );
        return new GatewayOutboundHttpResponse(
                response.status(),
                headers,
                response.body()
        );
    }

    private GatewayOutboundHttpResponse rpcError(
            HttpRpcUpstreamAdapter.HttpRpcUpstreamException failure,
            String traceId) {
        int status = switch (failure.status().getCode()) {
            case INVALID_ARGUMENT, FAILED_PRECONDITION -> 400;
            case UNAUTHENTICATED -> 401;
            case PERMISSION_DENIED -> 403;
            case NOT_FOUND -> 404;
            case ALREADY_EXISTS, ABORTED -> 409;
            case RESOURCE_EXHAUSTED -> 429;
            case DEADLINE_EXCEEDED -> 504;
            case UNAVAILABLE -> 503;
            default -> 502;
        };
        return error(
                status,
                "GATEWAY_RPC_UPSTREAM_"
                        + failure.status().getCode().name(),
                traceId
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

    private ProviderCallClassification classification(int status) {
        if (status < 400) {
            return ProviderCallClassification.SUCCESS;
        }
        return status >= 500
                ? ProviderCallClassification.RETRYABLE_FAILURE
                : ProviderCallClassification.BUSINESS_FAILURE;
    }

    private ProviderCallOutcome healthOutcome(
            ProviderCallClassification classification) {
        return switch (classification) {
            case SUCCESS -> ProviderCallOutcome.SUCCESS;
            case RETRYABLE_FAILURE ->
                    ProviderCallOutcome.RETRYABLE_FAILURE;
            case BUSINESS_FAILURE ->
                    ProviderCallOutcome.BUSINESS_REJECTION;
            case CANCELLED -> ProviderCallOutcome.CANCELLED;
        };
    }

    private boolean retryable(Throwable failure) {
        return failure instanceof RetryableHttpStatusException
                || failure instanceof java.io.IOException
                || failure instanceof java.util.concurrent.TimeoutException
                || failure instanceof java.net.ConnectException
                || failure.getCause() != null
                && failure.getCause() != failure
                && retryable(failure.getCause());
    }

    private boolean idempotent(
            HttpRouteMatch match,
            NormalizedHttpRequest request) {
        String configured = match.route().metadata().get("idempotent");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        return Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE")
                .contains(request.method());
    }

    private GatewayTrafficContext trafficContext(
            HttpRouteMatch match,
            NormalizedHttpRequest normalized,
            GatewayInboundHttpRequest request,
            GatewayHttpSecurityProcessor.Outcome security) {
        return new GatewayTrafficContext(
                match.route().operationId(),
                match.route().routeId(),
                match.route().metadata().getOrDefault(
                        "applicationCode",
                        match.route().gatewayGroupId()
                ),
                security.trustedIdentity().httpHeaders().get(
                        "X-Egon-Gateway-Principal-Id"
                ),
                request.remoteAddress() == null
                        ? null
                        : request.remoteAddress().getAddress()
                        .getHostAddress(),
                match.route().upstream().serviceName(),
                null,
                approvedHeaders(normalized.headers()),
                match.pathVariables(),
                queryParameters(normalized.rawQuery())
        );
    }

    private Map<String, String> approvedHeaders(
            Map<String, List<String>> headers) {
        Map<String, String> approved = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (!Set.of(
                    "authorization",
                    "proxy-authorization",
                    "cookie",
                    "set-cookie"
            ).contains(lower)
                    && values != null
                    && !values.isEmpty()) {
                approved.put(lower, values.getFirst());
            }
        });
        return Map.copyOf(approved);
    }

    private Map<String, String> queryParameters(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            String name = separator < 0
                    ? parameter
                    : parameter.substring(0, separator);
            String value = separator < 0
                    ? ""
                    : parameter.substring(separator + 1);
            values.putIfAbsent(
                    URLDecoder.decode(name, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return Map.copyOf(values);
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(
                0,
                (System.nanoTime() - startedNanos) / 1_000_000
        );
    }

    private static final class RetryableHttpStatusException
            extends RuntimeException {

        private final GatewayOutboundHttpResponse response;

        private RetryableHttpStatusException(
                GatewayOutboundHttpResponse response) {
            super("retryable HTTP status " + response.status());
            this.response = response;
        }

        private GatewayOutboundHttpResponse response() {
            return response;
        }
    }
}
