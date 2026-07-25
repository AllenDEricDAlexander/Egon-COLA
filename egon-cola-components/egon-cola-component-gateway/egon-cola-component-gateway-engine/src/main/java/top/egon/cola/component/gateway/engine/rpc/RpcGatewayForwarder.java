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
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficRejectedException;
import top.egon.cola.component.gateway.engine.traffic.ProviderCallClassification;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RpcGatewayForwarder {

    private final ProviderSelector providerSelector;

    private final RpcProviderChannelCache channels;

    private final Duration maximumTimeout;

    private final int maxInboundMessageBytes;

    private final GatewayRpcSecurityProcessor securityProcessor;

    private final GatewayCallCompletionListener completionListener;

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
            GatewayTraceContext trace = traceContext(inboundHeaders);
            GatewayCallObservation observation = GatewayCallObservation.start(
                    trace,
                    "RPC",
                    "INTERNAL",
                    engineNodeId
            );
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
            try {
                selection = providerSelector.select(
                        route.targetService(),
                        route.policyRefs()
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
                attemptSpanId = trace.newChildSpanId();
                channelHandle = channels.acquire(provider);
                clientCall = channelHandle.channel().newCall(
                        method,
                        callOptions(
                                trafficPermit.timeout(),
                                inboundDeadline
                        )
                );
                clientCall.start(new ClientCall.Listener<>() {
                    @Override
                    public void onHeaders(Metadata headers) {
                        serverCall.sendHeaders(safeMetadata(
                                headers,
                                trace.traceId()
                        ));
                    }

                    @Override
                    public void onMessage(byte[] message) {
                        observation.addResponseBytes(message.length);
                        serverCall.sendMessage(message);
                        clientCall.request(1);
                    }

                    @Override
                    public void onClose(
                            Status status,
                            Metadata trailers) {
                        try {
                            recordAttempt(status, null);
                            attemptPermit.complete(
                                    classification(status)
                            );
                            outcomeRecorder.record(
                                    selection.instance().runtimeIdentity(),
                                    healthOutcome(status)
                            );
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
                        } finally {
                            release();
                        }
                    }
                }, outboundHeaders(
                        route,
                        inboundHeaders,
                        trace,
                        attemptSpanId,
                        security
                ));
                clientCall.request(1);
                clientCall.sendMessage(request);
                clientCall.halfClose();
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
            if (channelHandle != null) {
                channelHandle.close();
            }
            if (selection != null) {
                selection.close();
            }
            if (attemptPermit != null) {
                attemptPermit.close();
            }
            if (trafficPermit != null) {
                trafficPermit.close();
            }
        }

        private void recordAttempt(Status status, String retryReason) {
            if (attemptSpanId == null) {
                return;
            }
            observation.attempt(
                    1,
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
            String childSpanId,
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
                trace.childTraceparent(childSpanId)
        );
        if (trace.tracestate() != null) {
            result.put(RpcMetadataKeys.TRACESTATE, trace.tracestate());
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
