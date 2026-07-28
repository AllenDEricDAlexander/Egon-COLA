package top.egon.cola.component.gateway.engine.http;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.context.GatewayStage;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
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
import top.egon.cola.component.gateway.engine.cors.RuntimeCorsPolicy;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallObservation;
import top.egon.cola.component.gateway.engine.observability.GatewayTelemetry;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final GatewayTelemetry telemetry;

    private final GatewayTrafficGovernance trafficGovernance;

    private final HttpRpcUpstreamAdapter httpRpcUpstream;

    private final ProviderCallOutcomeRecorder outcomeRecorder;

    private final GatewayAttemptExecutor attemptExecutor =
            new GatewayAttemptExecutor();

    private final String engineNodeId;

    private final String engineEnv;

    private final String engineNamespace;

    private final TrustedIdentitySanitizer identitySanitizer =
            new TrustedIdentitySanitizer();

    private final GatewayBodySizeLimiter bodySizeLimiter =
            new GatewayBodySizeLimiter();

    private final GatewayHttpExecutionPipeline executionPipeline =
            new GatewayHttpExecutionPipeline();

    private final GatewayCorsProcessor corsProcessor;

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
            String engineEnv,
            String engineNamespace) {
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
                ProviderCallOutcomeRecorder.noop(),
                Map::of,
                GatewayTelemetry.noop(),
                engineEnv,
                engineNamespace
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
                outcomeRecorder,
                Map::of
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
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies) {
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
                outcomeRecorder,
                corsPolicies,
                GatewayTelemetry.noop()
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
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies,
            GatewayTelemetry telemetry) {
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
                outcomeRecorder,
                corsPolicies,
                telemetry,
                "",
                ""
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
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies,
            GatewayTelemetry telemetry,
            String engineEnv,
            String engineNamespace) {
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
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.engineNodeId = Objects.requireNonNull(
                engineNodeId,
                "engineNodeId"
        );
        this.engineEnv = Objects.requireNonNull(engineEnv, "engineEnv");
        this.engineNamespace = Objects.requireNonNull(
                engineNamespace,
                "engineNamespace"
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
        corsProcessor = new GatewayCorsProcessor(
                Objects.requireNonNull(corsPolicies, "corsPolicies")
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
        GatewayTraceContext selectedTrace = traceContext(
                request.headers()
        );
        GatewayCallObservation observation = GatewayCallObservation.start(
                selectedTrace,
                "HTTP",
                accessZone.name(),
                engineNodeId,
                telemetry
        );
        observation.scope(engineEnv, engineNamespace);
        GatewayTraceContext trace = observation.trace();
        try {
            NormalizedHttpRequest normalized = normalizer.normalize(
                    request.method(),
                    request.host(),
                    request.uri(),
                    request.headers()
            );
            resourceGuard.validate(normalized);
            String routeMethod = routeMethod(request, normalized.method());
            HttpRouteMatch match = routeIndex.get().match(
                    normalized.host(),
                    routeMethod,
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
                    routeMethod,
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
            HttpStageExchange exchange = new HttpStageExchange(
                    accessZone,
                    request,
                    normalized,
                    match,
                    routeMethod,
                    trace,
                    observation
            );
            return executionPipeline.execute(exchange)
                    .map(response -> exchange.failed()
                            ? response
                            : observed(
                            response,
                            observation,
                            "COMPLETE",
                            category(response.status()),
                            response.status() >= 400
                                    ? "GATEWAY_UPSTREAM_STATUS"
                                    : null
                    ))
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

    private String routeMethod(
            GatewayInboundHttpRequest request,
            String method) {
        if (!"OPTIONS".equalsIgnoreCase(method)) {
            return method;
        }
        String requested = firstHeader(
                request.headers(),
                "access-control-request-method"
        );
        return requested == null || requested.isBlank()
                ? method
                : requested.trim().toUpperCase(java.util.Locale.ROOT);
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
        GatewayTelemetry.AttemptTrace attemptTrace =
                observation.beginAttempt(
                        attemptNumber,
                        provider.instanceId(),
                        provider.serviceKey().protocolType().name()
                );
        String attemptSpanId = attemptTrace.spanId();
        AttemptLifecycle lifecycle = new AttemptLifecycle(
                selection,
                attemptPermit,
                provider,
                observation,
                attemptNumber,
                attemptSpanId,
                attemptStartedAt,
                attemptStartedNanos,
                failedProviders
        );
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
                attemptTrace,
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
        Mono<GatewayOutboundHttpResponse> attemptResponse = invocation
                .map(response -> bodySizeLimiter.limitResponse(
                        response,
                        requestPermit.responseSizeLimit(maxResponseBytes)
                ))
                .flatMap(response -> {
                    GatewayOutboundHttpResponse tracked =
                            trackAttemptResponse(response, lifecycle);
                    boolean retryStatus = requestPermit.retryPolicy()
                            .retryableHttpStatus(response.status());
                    if (retryStatus
                            && attemptNumber
                            < requestPermit.retryPolicy().maxAttempts()) {
                        return tracked.body()
                                .then(Mono.error(
                                        new RetryableHttpStatusException(
                                                response.status()
                                        )
                                ));
                    }
                    return Mono.just(tracked);
                })
                .doOnError(lifecycle::fail);
        return handoffAttemptResponse(attemptResponse, lifecycle);
    }

    private Mono<GatewayOutboundHttpResponse> handoffAttemptResponse(
            Mono<GatewayOutboundHttpResponse> response,
            AttemptLifecycle lifecycle) {
        return Mono.create(sink -> {
            // MonoSink makes cancellation and response ownership transfer
            // mutually exclusive before the streamed body takes ownership.
            Disposable.Swap upstream = Disposables.swap();
            sink.onCancel(() -> {
                lifecycle.cancel();
                upstream.dispose();
            });
            Disposable subscription = response
                    .contextWrite(sink.currentContext())
                    .subscribe(
                            sink::success,
                            sink::error,
                            sink::success
                    );
            upstream.update(subscription);
        });
    }

    private GatewayOutboundHttpResponse trackAttemptResponse(
            GatewayOutboundHttpResponse response,
            AttemptLifecycle lifecycle) {
        return new GatewayOutboundHttpResponse(
                response.status(),
                response.headers(),
                response.body()
                        .doOnComplete(() ->
                                lifecycle.complete(response.status()))
                        .doOnError(lifecycle::fail)
                        .doOnCancel(lifecycle::cancel)
        );
    }

    private Map<String, List<String>> forwardedHeaders(
            Map<String, List<String>> source,
            GatewayTraceContext trace,
            GatewayTelemetry.AttemptTrace attemptTrace,
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
                List.of(attemptTrace.traceparent())
        );
        if (attemptTrace.tracestate() != null) {
            result.put(
                    "tracestate",
                    List.of(attemptTrace.tracestate())
            );
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

    private GatewayContext gatewayContext(
            AccessZone accessZone,
            HttpRouteMatch match,
            GatewayTraceContext trace) {
        Instant startedAt = Instant.now();
        return new GatewayContext(
                trace.traceId(),
                trace.traceId(),
                trace.engineTraceparent(),
                trace.tracestate(),
                accessZone,
                match.route().gatewayGroupId(),
                engineNodeId,
                match.route().operationId(),
                match.route().routeId(),
                match.route().metadata().get("releaseId"),
                null,
                null,
                startedAt.plus(upstreamTimeout),
                startedAt,
                GatewayStage.ROUTE_MATCHED,
                List.of(),
                List.of()
        );
    }

    private final class HttpStageExchange
            extends AbstractGatewayHttpStageExchange {

        private final AccessZone accessZone;

        private final NormalizedHttpRequest normalized;

        private final HttpRouteMatch match;

        private final String routeMethod;

        private final GatewayTraceContext trace;

        private final GatewayCallObservation observation;

        private GatewayCorsProcessor.Decision cors;

        private GatewayHttpSecurityProcessor.Outcome security;

        private GatewayTrafficGovernance.RequestPermit permit;

        private boolean failed;

        private HttpStageExchange(
                AccessZone accessZone,
                GatewayInboundHttpRequest request,
                NormalizedHttpRequest normalized,
                HttpRouteMatch match,
                String routeMethod,
                GatewayTraceContext trace,
                GatewayCallObservation observation) {
            super(request, gatewayContext(accessZone, match, trace));
            this.accessZone = accessZone;
            this.normalized = normalized;
            this.match = match;
            this.routeMethod = routeMethod;
            this.trace = trace;
            this.observation = observation;
        }

        @Override
        public Publisher<GatewayResponse> cors(
                GatewayFilterChain chain) {
            cors = corsProcessor.evaluate(
                    match.route().policyRefs(),
                    inbound(),
                    routeMethod,
                    trace.traceId()
            );
            return cors.preflightResponse()
                    .<Publisher<GatewayResponse>>map(this::respond)
                    .orElseGet(() -> chain.filter(this));
        }

        @Override
        public Publisher<GatewayResponse> security(
                GatewayFilterChain chain) {
            return securityProcessor.authorize(
                            accessZone,
                            inbound(),
                            normalized,
                            match,
                            trace.traceId()
                    )
                    .flatMap(outcome -> {
                        security = outcome;
                        return Mono.from(chain.filter(this));
                    });
        }

        @Override
        public Publisher<GatewayResponse> governance(
                GatewayFilterChain chain) {
            GatewayTrafficContext context = trafficContext(
                    match,
                    normalized,
                    inbound(),
                    security
            );
            return trafficGovernance.acquire(
                            match.route().policyRefs(),
                            context,
                            upstreamTimeout
                    )
                    .flatMap(acquired -> {
                        permit = acquired;
                        return Mono.from(chain.filter(this))
                                .doFinally(signal -> acquired.close());
                    });
        }

        @Override
        public Publisher<GatewayResponse> invoke() {
            return invokeUpstream(
                    match,
                    normalized,
                    inbound(),
                    security,
                    trace,
                    observation,
                    permit
            )
                    .map(cors::decorate)
                    .flatMap(response -> Mono.from(respond(response)));
        }

        @Override
        public GatewayOutboundHttpResponse mapFailure(Throwable failure) {
            failed = true;
            if (failure instanceof GatewaySecurityException rejected) {
                return observed(
                        error(
                                rejected.httpStatus(),
                                rejected.code(),
                                trace.traceId()
                        ),
                        observation,
                        "SECURITY",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayCorsException rejected) {
                return observed(
                        error(403, rejected.code(), trace.traceId()),
                        observation,
                        "CORS",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayRequestRejectedException rejected) {
                return observed(
                        error(
                                rejected.status(),
                                rejected.code(),
                                trace.traceId()
                        ),
                        observation,
                        "RESOURCE",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayRequestBodyTooLargeException
                    rejected) {
                return observed(
                        error(413, rejected.code(), trace.traceId()),
                        observation,
                        "RESOURCE",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayResponseBodyTooLargeException
                    rejected) {
                return observed(
                        error(502, rejected.code(), trace.traceId()),
                        observation,
                        "UPSTREAM",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayTrafficRejectedException rejected) {
                observation.governance(
                        "APPLIED",
                        rejected.code(),
                        "REJECT"
                );
                return observed(
                        trafficError(rejected, trace.traceId()),
                        observation,
                        "GOVERNANCE",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof java.util.concurrent.TimeoutException) {
                return observed(
                        error(
                                504,
                                "GATEWAY_UPSTREAM_TIMEOUT",
                                trace.traceId()
                        ),
                        observation,
                        "UPSTREAM",
                        "TIMEOUT",
                        "GATEWAY_UPSTREAM_TIMEOUT"
                );
            }
            return observed(
                    error(
                            502,
                            "GATEWAY_UPSTREAM_CONNECT_FAILED",
                            trace.traceId()
                    ),
                    observation,
                    "UPSTREAM",
                    "ERROR",
                    "GATEWAY_UPSTREAM_CONNECT_FAILED"
            );
        }

        private boolean failed() {
            return failed;
        }
    }

    private static final class RetryableHttpStatusException
            extends RuntimeException {

        private RetryableHttpStatusException(int status) {
            super("retryable HTTP status " + status);
        }
    }

    private final class AttemptLifecycle {

        private final AtomicBoolean completed = new AtomicBoolean();

        private final ProviderSelectionHandle selection;

        private final GatewayTrafficGovernance.AttemptPermit permit;

        private final ProviderInstance provider;

        private final GatewayCallObservation observation;

        private final int attemptNumber;

        private final String attemptSpanId;

        private final long attemptStartedAt;

        private final long attemptStartedNanos;

        private final Set<String> failedProviders;

        private AttemptLifecycle(
                ProviderSelectionHandle selection,
                GatewayTrafficGovernance.AttemptPermit permit,
                ProviderInstance provider,
                GatewayCallObservation observation,
                int attemptNumber,
                String attemptSpanId,
                long attemptStartedAt,
                long attemptStartedNanos,
                Set<String> failedProviders) {
            this.selection = selection;
            this.permit = permit;
            this.provider = provider;
            this.observation = observation;
            this.attemptNumber = attemptNumber;
            this.attemptSpanId = attemptSpanId;
            this.attemptStartedAt = attemptStartedAt;
            this.attemptStartedNanos = attemptStartedNanos;
            this.failedProviders = failedProviders;
        }

        private void complete(int status) {
            ProviderCallClassification classification =
                    classification(status);
            finish(
                    classification,
                    category(status),
                    null,
                    classification
                            == ProviderCallClassification.RETRYABLE_FAILURE
            );
        }

        private void fail(Throwable failure) {
            finish(
                    ProviderCallClassification.RETRYABLE_FAILURE,
                    "ERROR",
                    retryable(failure)
                            ? "RETRYABLE_UPSTREAM_FAILURE"
                            : null,
                    true
            );
        }

        private void cancel() {
            finish(
                    ProviderCallClassification.CANCELLED,
                    "CANCELLED",
                    "GATEWAY_CLIENT_CANCELLED",
                    false
            );
        }

        private void finish(
                ProviderCallClassification classification,
                String category,
                String retryReason,
                boolean failedProvider) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            try {
                permit.complete(classification);
                outcomeRecorder.record(
                        provider.runtimeIdentity(),
                        healthOutcome(classification)
                );
                if (failedProvider) {
                    failedProviders.add(provider.runtimeIdentity());
                }
                observation.attempt(
                        attemptNumber,
                        attemptSpanId,
                        provider.instanceId(),
                        attemptStartedAt,
                        elapsedMillis(attemptStartedNanos),
                        category,
                        retryReason
                );
            } finally {
                selection.close();
            }
        }
    }
}
