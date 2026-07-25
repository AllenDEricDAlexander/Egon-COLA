package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcome;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcomeRecorder;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallObservation;
import top.egon.cola.component.gateway.engine.observability.GatewayTelemetry;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficRejectedException;
import top.egon.cola.component.gateway.engine.traffic.ProviderCallClassification;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RpcGatewayForwarder {

    private final ProviderSelector providerSelector;

    private final RpcProviderChannelCache channels;

    private final Duration maximumTimeout;

    private final int maxInboundMessageBytes;

    private final GatewayRpcSecurityProcessor securityProcessor;

    private final GatewayCallCompletionListener completionListener;

    private final GatewayTelemetry telemetry;

    private final String engineNodeId;

    private final GatewayTrafficGovernance trafficGovernance;

    private final ProviderCallOutcomeRecorder outcomeRecorder;

    private final TrustedIdentitySanitizer identitySanitizer =
            new TrustedIdentitySanitizer();

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                (route, metadata, traceId, deadline) ->
                        reactor.core.publisher.Mono.just(
                                GatewayRpcSecurityProcessor.Outcome.anonymous()
                ),
                GatewayCallCompletionListener.noop(),
                "unknown-engine",
                GatewayTrafficGovernance.noop()
        );
    }

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                GatewayCallCompletionListener.noop(),
                "unknown-engine",
                GatewayTrafficGovernance.noop()
        );
    }

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                completionListener,
                engineNodeId,
                GatewayTrafficGovernance.noop()
        );
    }

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                ProviderCallOutcomeRecorder.noop()
        );
    }

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            ProviderCallOutcomeRecorder outcomeRecorder) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                outcomeRecorder,
                GatewayTelemetry.noop()
        );
    }

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            ProviderCallOutcomeRecorder outcomeRecorder,
            GatewayTelemetry telemetry) {
        this.providerSelector = Objects.requireNonNull(
                providerSelector,
                "providerSelector"
        );
        this.channels = Objects.requireNonNull(channels, "channels");
        this.maximumTimeout = Objects.requireNonNull(
                maximumTimeout,
                "maximumTimeout"
        );
        this.maxInboundMessageBytes = maxInboundMessageBytes;
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
        this.trafficGovernance = Objects.requireNonNull(
                trafficGovernance,
                "trafficGovernance"
        );
        this.outcomeRecorder = Objects.requireNonNull(
                outcomeRecorder,
                "outcomeRecorder"
        );
    }

    public ServerCallHandler<byte[], byte[]> handler(RuntimeRpcRoute route) {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor(route.fullMethodName());
        return (serverCall, inboundHeaders) -> {
            GatewayTraceContext selectedTrace = traceContext(
                    inboundHeaders
            );
            GatewayCallObservation observation = GatewayCallObservation.start(
                    selectedTrace,
                    "RPC",
                    "INTERNAL",
                    engineNodeId,
                    telemetry
            );
            GatewayTraceContext trace = observation.trace();
            observation.route(
                    route.fullMethodName(),
                    route.fullMethodName(),
                    route.targetService().group(),
                    null,
                    route.operationId(),
                    route.routeId()
            );
            observation.scope(
                    route.targetService().env(),
                    route.targetService().namespace()
            );
            if (!metadataMatches(route, inboundHeaders)) {
                serverCall.close(
                        Status.INVALID_ARGUMENT.withDescription(
                                "RPC method metadata conflicts with route"
                        ),
                        gatewayTrailers(
                                "GATEWAY_RPC_METADATA_MISMATCH",
                                trace.traceId()
                        )
                );
                publish(
                        observation,
                        "ROUTE",
                        Status.INVALID_ARGUMENT,
                        "GATEWAY_RPC_METADATA_MISMATCH"
                );
                return new ServerCall.Listener<>() {
                };
            }
            PendingCall pending = new PendingCall(
                    route,
                    method,
                    serverCall,
                    inboundHeaders,
                    trace,
                    Context.current().getDeadline(),
                    observation
            );
            securityProcessor.authorize(
                            route,
                            inboundHeaders,
                            trace.traceId(),
                            Context.current().getDeadline()
                    )
                    .subscribe(pending::authorized, pending::securityFailed);
            serverCall.request(1);
            return pending;
        };
    }

    private final class PendingCall extends ServerCall.Listener<byte[]> {

        private final RuntimeRpcRoute route;

        private final MethodDescriptor<byte[], byte[]> method;

        private final ServerCall<byte[], byte[]> serverCall;

        private final Metadata inboundHeaders;

        private final GatewayTraceContext trace;

        private final Deadline inboundDeadline;

        private final GatewayCallObservation observation;

        private final AtomicBoolean released = new AtomicBoolean();

        private byte[] request;

        private boolean halfClosed;

        private boolean cancelled;

        private boolean started;

        private GatewayRpcSecurityProcessor.Outcome security;

        private GatewayTrafficGovernance.RequestPermit trafficPermit;

        private GatewayTrafficGovernance.AttemptPermit attemptPermit;

        private ProviderSelectionHandle selection;

        private RpcProviderChannelCache.ChannelHandle channelHandle;

        private ClientCall<byte[], byte[]> clientCall;

        private long attemptStartedAt;

        private long attemptStartedNanos;

        private String attemptSpanId;

        private int attemptNumber;

        private long retryDeadlineNanos;

        private boolean responseStarted;

        private final Set<String> failedProviders = new LinkedHashSet<>();

        private PendingCall(
                RuntimeRpcRoute route,
                MethodDescriptor<byte[], byte[]> method,
                ServerCall<byte[], byte[]> serverCall,
                Metadata inboundHeaders,
                GatewayTraceContext trace,
                Deadline inboundDeadline,
                GatewayCallObservation observation) {
            this.route = route;
            this.method = method;
            this.serverCall = serverCall;
            this.inboundHeaders = inboundHeaders;
            this.trace = trace;
            this.inboundDeadline = inboundDeadline;
            this.observation = observation;
        }

        @Override
        public synchronized void onMessage(byte[] message) {
            if (message.length > maxInboundMessageBytes) {
                close(
                        Status.RESOURCE_EXHAUSTED,
                        "GATEWAY_RPC_MESSAGE_TOO_LARGE"
                );
                return;
            }
            if (request != null) {
                close(
                        Status.INVALID_ARGUMENT,
                        "GATEWAY_RPC_MULTIPLE_MESSAGES"
                );
                return;
            }
            request = message;
            observation.addRequestBytes(message.length);
            startIfReady();
        }

        @Override
        public synchronized void onHalfClose() {
            halfClosed = true;
            if (request == null) {
                close(
                        Status.INVALID_ARGUMENT,
                        "GATEWAY_RPC_REQUEST_MISSING"
                );
                return;
            }
            startIfReady();
        }

        @Override
        public synchronized void onCancel() {
            cancelled = true;
            if (clientCall != null) {
                clientCall.cancel("consumer cancelled", null);
            }
            publish(
                    observation,
                    "CLIENT",
                    Status.CANCELLED,
                    "GATEWAY_RPC_CANCELLED"
            );
            release();
        }

        private synchronized void authorized(
                GatewayRpcSecurityProcessor.Outcome outcome) {
            if (cancelled || released.get()) {
                return;
            }
            security = Objects.requireNonNull(outcome, "security outcome");
            trafficGovernance.acquire(
                            route.policyRefs(),
                            trafficContext(route, inboundHeaders, security),
                            route.timeout()
                    )
                    .subscribe(
                            permit -> {
                                synchronized (PendingCall.this) {
                                    if (cancelled || released.get()) {
                                        permit.close();
                                        return;
                                    }
                                    trafficPermit = permit;
                                    observation.governance(
                                            "APPLIED",
                                            permit.retryPolicy().enabled()
                                                    ? "RETRY_ENABLED"
                                                    : "RETRY_DISABLED",
                                            "ALLOW"
                                    );
                                    startIfReady();
                                }
                            },
                            this::trafficFailed
                    );
        }

        private synchronized void securityFailed(Throwable failure) {
            if (failure instanceof GatewaySecurityException securityFailure) {
                close(
                        rpcStatus(securityFailure.rpcStatus()),
                        securityFailure.code()
                );
                return;
            }
            close(
                    Status.UNAVAILABLE,
                    "GATEWAY_SECURITY_PROVIDER_ERROR"
            );
        }

        private synchronized void trafficFailed(Throwable failure) {
            if (failure instanceof GatewayTrafficRejectedException rejected) {
                observation.governance(
                        "APPLIED",
                        rejected.code(),
                        "REJECT"
                );
                close(
                        rpcStatus(rejected.rpcStatus()),
                        rejected.code()
                );
                return;
            }
            close(
                    Status.UNAVAILABLE,
                    "GATEWAY_GOVERNANCE_UNAVAILABLE"
            );
        }

        private void startIfReady() {
            if (started
                    || cancelled
                    || security == null
                    || trafficPermit == null
                    || request == null
                    || !halfClosed) {
                return;
            }
            started = true;
            retryDeadlineNanos = System.nanoTime()
                    + totalBudgetNanos(
                    trafficPermit.timeout(),
                    inboundDeadline
            );
            startAttempt();
        }

        private synchronized void startAttempt() {
            if (cancelled || released.get()) {
                return;
            }
            attemptNumber++;
            responseStarted = false;
            try {
                selection = providerSelector.select(
                        route.targetService(),
                        route.policyRefs(),
                        failedProviders
                );
                ProviderInstance provider = selection.instance();
                attemptPermit = trafficPermit.acquireAttempt(provider);
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
                attemptStartedAt = System.currentTimeMillis();
                attemptStartedNanos = System.nanoTime();
                GatewayTelemetry.AttemptTrace attemptTrace =
                        observation.beginAttempt(
                                attemptNumber,
                                provider.instanceId(),
                                provider.serviceKey()
                                        .protocolType()
                                        .name()
                        );
                attemptSpanId = attemptTrace.spanId();
                channelHandle = channels.acquire(provider);
                clientCall = channelHandle.channel().newCall(
                        method,
                        callOptions(
                                remainingBudget(),
                                inboundDeadline
                        )
                );
                ClientCall<byte[], byte[]> activeCall = clientCall;
                activeCall.start(new ClientCall.Listener<>() {
                    @Override
                    public void onHeaders(Metadata headers) {
                        synchronized (PendingCall.this) {
                            if (activeCall != clientCall
                                    || released.get()) {
                                return;
                            }
                            responseStarted = true;
                            serverCall.sendHeaders(safeMetadata(
                                    headers,
                                    trace.traceId()
                            ));
                        }
                    }

                    @Override
                    public void onMessage(byte[] message) {
                        synchronized (PendingCall.this) {
                            if (activeCall != clientCall
                                    || released.get()) {
                                return;
                            }
                            responseStarted = true;
                            observation.addResponseBytes(message.length);
                            serverCall.sendMessage(message);
                            activeCall.request(1);
                        }
                    }

                    @Override
                    public void onClose(
                            Status status,
                            Metadata trailers) {
                        synchronized (PendingCall.this) {
                            if (activeCall != clientCall
                                    || released.get()) {
                                return;
                            }
                            boolean retry = shouldRetry(status);
                            recordAttempt(
                                    status,
                                    retry
                                            ? "RETRYABLE_RPC_STATUS"
                                            : null
                            );
                            attemptPermit.complete(
                                    classification(status)
                            );
                            attemptPermit = null;
                            outcomeRecorder.record(
                                    selection.instance().runtimeIdentity(),
                                    healthOutcome(status)
                            );
                            if (retry) {
                                failedProviders.add(
                                        selection.instance()
                                                .runtimeIdentity()
                                );
                                closeAttemptHandles();
                                scheduleRetry();
                                return;
                            }
                            serverCall.close(
                                    status,
                                    safeMetadata(
                                            trailers,
                                            trace.traceId()
                                    )
                            );
                            publish(
                                    observation,
                                    "COMPLETE",
                                    status,
                                    status.isOk()
                                            ? null
                                            : "GATEWAY_RPC_UPSTREAM_STATUS"
                            );
                            release();
                        }
                    }
                }, outboundHeaders(
                        route,
                        inboundHeaders,
                        trace,
                        attemptTrace,
                        security
                ));
                activeCall.request(1);
                activeCall.sendMessage(request);
                activeCall.halfClose();
            } catch (RuntimeException unavailable) {
                if (selection != null) {
                    outcomeRecorder.record(
                            selection.instance().runtimeIdentity(),
                            ProviderCallOutcome.RETRYABLE_FAILURE
                    );
                }
                recordAttempt(
                        Status.UNAVAILABLE,
                        "GATEWAY_PROVIDER_UNAVAILABLE"
                );
                close(
                        Status.UNAVAILABLE,
                        "GATEWAY_PROVIDER_UNAVAILABLE"
                );
            }
        }

        private boolean shouldRetry(Status status) {
            var policy = trafficPermit.retryPolicy();
            if (!policy.enabled()
                    || !route.idempotent()
                    || responseStarted
                    || attemptNumber >= policy.maxAttempts()
                    || !policy.retryableRpcStatus(
                    status.getCode().name()
            )) {
                return false;
            }
            long required = policy.backoff(attemptNumber).toNanos()
                    + policy.minimumAttemptBudget().toNanos();
            return retryDeadlineNanos - System.nanoTime() >= required;
        }

        private void scheduleRetry() {
            Duration backoff = trafficPermit.retryPolicy().backoff(
                    attemptNumber
            );
            reactor.core.publisher.Mono.delay(backoff).subscribe(ignored -> {
                synchronized (PendingCall.this) {
                    startAttempt();
                }
            });
        }

        private Duration remainingBudget() {
            return Duration.ofNanos(Math.max(
                    1,
                    retryDeadlineNanos - System.nanoTime()
            ));
        }

        private void close(Status status, String code) {
            if (released.compareAndSet(false, true)) {
                if (clientCall != null) {
                    clientCall.cancel(code, null);
                }
                serverCall.close(
                        status.withDescription(code),
                        gatewayTrailers(code, trace.traceId())
                );
                publish(observation, "GATEWAY", status, code);
                closeHandles();
            }
        }

        private void release() {
            if (released.compareAndSet(false, true)) {
                closeHandles();
            }
        }

        private void closeHandles() {
            closeAttemptHandles();
            if (trafficPermit != null) {
                trafficPermit.close();
                trafficPermit = null;
            }
        }

        private void closeAttemptHandles() {
            if (channelHandle != null) {
                channelHandle.close();
                channelHandle = null;
            }
            if (selection != null) {
                selection.close();
                selection = null;
            }
            if (attemptPermit != null) {
                attemptPermit.close();
                attemptPermit = null;
            }
            clientCall = null;
        }

        private void recordAttempt(Status status, String retryReason) {
            if (attemptSpanId == null) {
                return;
            }
            observation.attempt(
                    attemptNumber,
                    attemptSpanId,
                    selection == null
                            ? null
                            : selection.instance().instanceId(),
                    attemptStartedAt,
                    Math.max(
                            0,
                            (System.nanoTime() - attemptStartedNanos)
                                    / 1_000_000
                    ),
                    status.isOk() ? "SUCCESS" : "ERROR",
                    retryReason
            );
            attemptSpanId = null;
        }
    }

    private CallOptions callOptions(
            Duration routeTimeout,
            Deadline inboundDeadline) {
        long remainingNanos = maximumTimeout.toNanos();
        if (inboundDeadline != null) {
            remainingNanos = Math.min(
                    remainingNanos,
                    inboundDeadline.timeRemaining(TimeUnit.NANOSECONDS)
            );
        }
        remainingNanos = Math.min(remainingNanos, routeTimeout.toNanos());
        return CallOptions.DEFAULT.withDeadlineAfter(
                Math.max(1, remainingNanos),
                TimeUnit.NANOSECONDS
        );
    }

    private long totalBudgetNanos(
            Duration routeTimeout,
            Deadline inboundDeadline) {
        long result = Math.min(
                maximumTimeout.toNanos(),
                routeTimeout.toNanos()
        );
        if (inboundDeadline != null) {
            result = Math.min(
                    result,
                    inboundDeadline.timeRemaining(TimeUnit.NANOSECONDS)
            );
        }
        return Math.max(1, result);
    }

    private boolean metadataMatches(
            RuntimeRpcRoute route,
            Metadata metadata) {
        return matches(
                metadata.get(RpcMetadataKeys.SERVICE),
                route.targetService().serviceName()
        ) && matches(
                metadata.get(RpcMetadataKeys.GROUP),
                route.targetService().group()
        ) && matches(
                metadata.get(RpcMetadataKeys.VERSION),
                route.targetService().version()
        );
    }

    private boolean matches(String supplied, String expected) {
        return supplied == null || supplied.equals(expected);
    }

    private Metadata outboundHeaders(
            RuntimeRpcRoute route,
            Metadata inbound,
            GatewayTraceContext trace,
            GatewayTelemetry.AttemptTrace attemptTrace,
            GatewayRpcSecurityProcessor.Outcome security) {
        Metadata result = new Metadata();
        result.put(
                RpcMetadataKeys.SERVICE,
                route.targetService().serviceName()
        );
        result.put(RpcMetadataKeys.GROUP, route.targetService().group());
        result.put(RpcMetadataKeys.VERSION, route.targetService().version());
        result.put(
                RpcMetadataKeys.INVOCATION_ID,
                valueOrGenerated(inbound.get(RpcMetadataKeys.INVOCATION_ID))
        );
        result.put(RpcMetadataKeys.TRACE_ID, trace.traceId());
        result.put(
                RpcMetadataKeys.TRACEPARENT,
                attemptTrace.traceparent()
        );
        if (attemptTrace.tracestate() != null) {
            result.put(
                    RpcMetadataKeys.TRACESTATE,
                    attemptTrace.tracestate()
            );
        }
        copy(inbound, result, RpcMetadataKeys.SOURCE_APP);
        copy(inbound, result, RpcMetadataKeys.SOURCE_INSTANCE);
        Map<String, String> trusted = identitySanitizer.sanitizeRpc(
                Map.of(),
                security.fieldsToRemove(),
                security.trustedIdentity()
        );
        trusted.forEach((name, value) -> result.put(
                Metadata.Key.of(
                        name,
                        Metadata.ASCII_STRING_MARSHALLER
                ),
                value
        ));
        return result;
    }

    private void copy(
            Metadata source,
            Metadata target,
            Metadata.Key<String> key) {
        String value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private Metadata safeMetadata(Metadata source, String traceId) {
        Metadata safe = new Metadata();
        copy(source, safe, RpcMetadataKeys.FAILURE_STAGE);
        safe.put(RpcMetadataKeys.TRACE_ID, traceId);
        return safe;
    }

    private Metadata gatewayTrailers(String errorCode, String traceId) {
        Metadata trailers = new Metadata();
        trailers.put(RpcMetadataKeys.FAILURE_STAGE, errorCode);
        trailers.put(RpcMetadataKeys.TRACE_ID, traceId);
        return trailers;
    }

    private GatewayTraceContext traceContext(Metadata metadata) {
        return GatewayTraceContext.select(
                metadata.get(RpcMetadataKeys.TRACEPARENT),
                metadata.get(RpcMetadataKeys.TRACE_ID),
                metadata.get(RpcMetadataKeys.TRACESTATE)
        );
    }

    private String valueOrGenerated(String value) {
        return value == null || value.isBlank()
                ? UuidV7.simpleString()
                : value;
    }

    private Status rpcStatus(String value) {
        return switch (value) {
            case "UNAUTHENTICATED" -> Status.UNAUTHENTICATED;
            case "PERMISSION_DENIED" -> Status.PERMISSION_DENIED;
            case "INTERNAL" -> Status.INTERNAL;
            case "RESOURCE_EXHAUSTED" -> Status.RESOURCE_EXHAUSTED;
            case "DEADLINE_EXCEEDED" -> Status.DEADLINE_EXCEEDED;
            default -> Status.UNAVAILABLE;
        };
    }

    private ProviderCallClassification classification(Status status) {
        if (status.isOk()) {
            return ProviderCallClassification.SUCCESS;
        }
        return switch (status.getCode()) {
            case INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS,
                    FAILED_PRECONDITION, UNAUTHENTICATED,
                    PERMISSION_DENIED ->
                    ProviderCallClassification.BUSINESS_FAILURE;
            case CANCELLED -> ProviderCallClassification.CANCELLED;
            default -> ProviderCallClassification.RETRYABLE_FAILURE;
        };
    }

    private ProviderCallOutcome healthOutcome(Status status) {
        return switch (classification(status)) {
            case SUCCESS -> ProviderCallOutcome.SUCCESS;
            case RETRYABLE_FAILURE ->
                    ProviderCallOutcome.RETRYABLE_FAILURE;
            case BUSINESS_FAILURE ->
                    ProviderCallOutcome.BUSINESS_REJECTION;
            case CANCELLED -> ProviderCallOutcome.CANCELLED;
        };
    }

    private GatewayTrafficContext trafficContext(
            RuntimeRpcRoute route,
            Metadata metadata,
            GatewayRpcSecurityProcessor.Outcome security) {
        return new GatewayTrafficContext(
                route.operationId(),
                route.routeId(),
                valueOrGenerated(metadata.get(RpcMetadataKeys.SOURCE_APP)),
                security.trustedIdentity().rpcMetadata().get(
                        "egon-gateway-principal-id"
                ),
                null,
                route.targetService().serviceName(),
                null,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    private void publish(
            GatewayCallObservation observation,
            String stage,
            Status status,
            String code) {
        observation.complete(
                stage,
                status.isOk()
                        ? "SUCCESS"
                        : status.getCode() == Status.Code.CANCELLED
                        ? "CANCELLED"
                        : status.getCode() == Status.Code.DEADLINE_EXCEEDED
                        ? "TIMEOUT"
                        : status.getCode() == Status.Code.PERMISSION_DENIED
                        || status.getCode() == Status.Code.UNAUTHENTICATED
                        || status.getCode() == Status.Code.INVALID_ARGUMENT
                        ? "REJECTED"
                        : "ERROR",
                code,
                null,
                status.getCode().name()
        ).ifPresent(completionListener::onComplete);
    }
}
