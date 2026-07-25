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
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
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
                        )
        );
    }

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor) {
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
    }

    public ServerCallHandler<byte[], byte[]> handler(RuntimeRpcRoute route) {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor(route.fullMethodName());
        return (serverCall, inboundHeaders) -> {
            String traceId = traceId(inboundHeaders);
            if (!metadataMatches(route, inboundHeaders)) {
                serverCall.close(
                        Status.INVALID_ARGUMENT.withDescription(
                                "RPC method metadata conflicts with route"
                        ),
                        new Metadata()
                );
                return new ServerCall.Listener<>() {
                };
            }
            PendingCall pending = new PendingCall(
                    route,
                    method,
                    serverCall,
                    inboundHeaders,
                    traceId,
                    Context.current().getDeadline()
            );
            securityProcessor.authorize(
                            route,
                            inboundHeaders,
                            traceId,
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

        private final String traceId;

        private final Deadline inboundDeadline;

        private final AtomicBoolean released = new AtomicBoolean();

        private byte[] request;

        private boolean halfClosed;

        private boolean cancelled;

        private boolean started;

        private GatewayRpcSecurityProcessor.Outcome security;

        private ProviderSelectionHandle selection;

        private RpcProviderChannelCache.ChannelHandle channelHandle;

        private ClientCall<byte[], byte[]> clientCall;

        private PendingCall(
                RuntimeRpcRoute route,
                MethodDescriptor<byte[], byte[]> method,
                ServerCall<byte[], byte[]> serverCall,
                Metadata inboundHeaders,
                String traceId,
                Deadline inboundDeadline) {
            this.route = route;
            this.method = method;
            this.serverCall = serverCall;
            this.inboundHeaders = inboundHeaders;
            this.traceId = traceId;
            this.inboundDeadline = inboundDeadline;
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
            release();
        }

        private synchronized void authorized(
                GatewayRpcSecurityProcessor.Outcome outcome) {
            if (cancelled || released.get()) {
                return;
            }
            security = Objects.requireNonNull(outcome, "security outcome");
            startIfReady();
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

        private void startIfReady() {
            if (started
                    || cancelled
                    || security == null
                    || request == null
                    || !halfClosed) {
                return;
            }
            started = true;
            try {
                selection = providerSelector.select(route.targetService());
                ProviderInstance provider = selection.instance();
                channelHandle = channels.acquire(provider);
                clientCall = channelHandle.channel().newCall(
                        method,
                        callOptions(route.timeout(), inboundDeadline)
                );
                clientCall.start(new ClientCall.Listener<>() {
                    @Override
                    public void onHeaders(Metadata headers) {
                        serverCall.sendHeaders(safeMetadata(headers));
                    }

                    @Override
                    public void onMessage(byte[] message) {
                        serverCall.sendMessage(message);
                        clientCall.request(1);
                    }

                    @Override
                    public void onClose(
                            Status status,
                            Metadata trailers) {
                        try {
                            serverCall.close(
                                    status,
                                    safeMetadata(trailers)
                            );
                        } finally {
                            release();
                        }
                    }
                }, outboundHeaders(
                        route,
                        inboundHeaders,
                        traceId,
                        security
                ));
                clientCall.request(1);
                clientCall.sendMessage(request);
                clientCall.halfClose();
            } catch (RuntimeException unavailable) {
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
                        gatewayTrailers(code, traceId)
                );
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
            String traceId,
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
        result.put(RpcMetadataKeys.TRACE_ID, traceId);
        copy(inbound, result, RpcMetadataKeys.TRACEPARENT);
        copy(inbound, result, RpcMetadataKeys.TRACESTATE);
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

    private Metadata safeMetadata(Metadata source) {
        Metadata safe = new Metadata();
        copy(source, safe, RpcMetadataKeys.FAILURE_STAGE);
        copy(source, safe, RpcMetadataKeys.TRACE_ID);
        return safe;
    }

    private Metadata gatewayTrailers(String errorCode, String traceId) {
        Metadata trailers = new Metadata();
        trailers.put(RpcMetadataKeys.FAILURE_STAGE, errorCode);
        trailers.put(RpcMetadataKeys.TRACE_ID, traceId);
        return trailers;
    }

    private String traceId(Metadata metadata) {
        return valueOrGenerated(metadata.get(RpcMetadataKeys.TRACE_ID));
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
            default -> Status.UNAVAILABLE;
        };
    }
}
