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
import top.egon.cola.component.rpc.context.RpcMetadataKeys;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class RpcGatewayForwarder {

    private final ProviderSelector providerSelector;

    private final RpcProviderChannelCache channels;

    private final Duration maximumTimeout;

    private final int maxInboundMessageBytes;

    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes) {
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
    }

    public ServerCallHandler<byte[], byte[]> handler(RuntimeRpcRoute route) {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor(route.fullMethodName());
        return (serverCall, inboundHeaders) -> {
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
            ProviderSelectionHandle selection;
            try {
                selection = providerSelector.select(route.targetService());
            } catch (RuntimeException unavailable) {
                serverCall.close(
                        Status.UNAVAILABLE.withDescription(
                                "GATEWAY_PROVIDER_UNAVAILABLE"
                        ),
                        gatewayTrailers(
                                "GATEWAY_PROVIDER_UNAVAILABLE",
                                traceId(inboundHeaders)
                        )
                );
                return new ServerCall.Listener<>() {
                };
            }
            ProviderInstance provider = selection.instance();
            RpcProviderChannelCache.ChannelHandle handle =
                    channels.acquire(provider);
            ClientCall<byte[], byte[]> clientCall = handle.channel().newCall(
                    method,
                    callOptions(route.timeout())
            );
            String traceId = traceId(inboundHeaders);
            Metadata outboundHeaders = outboundHeaders(
                    route,
                    inboundHeaders,
                    traceId
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
                public void onClose(Status status, Metadata trailers) {
                    try {
                        serverCall.close(status, safeMetadata(trailers));
                    } finally {
                        handle.close();
                        selection.close();
                    }
                }
            }, outboundHeaders);
            clientCall.request(1);
            ServerCall.Listener<byte[]> listener =
                    new ServerCall.Listener<>() {
                private byte[] request;

                @Override
                public void onMessage(byte[] message) {
                    if (message.length > maxInboundMessageBytes) {
                        clientCall.cancel("message too large", null);
                        serverCall.close(
                                Status.RESOURCE_EXHAUSTED.withDescription(
                                        "GATEWAY_RPC_MESSAGE_TOO_LARGE"
                                ),
                                gatewayTrailers(
                                        "GATEWAY_RPC_MESSAGE_TOO_LARGE",
                                        traceId
                                )
                        );
                        handle.close();
                        selection.close();
                        return;
                    }
                    if (request != null) {
                        clientCall.cancel("more than one unary message", null);
                        serverCall.close(
                                Status.INVALID_ARGUMENT.withDescription(
                                        "unary call has multiple messages"
                                ),
                                new Metadata()
                        );
                        handle.close();
                        selection.close();
                        return;
                    }
                    request = message;
                }

                @Override
                public void onHalfClose() {
                    if (request == null) {
                        clientCall.cancel("missing request", null);
                        serverCall.close(
                                Status.INVALID_ARGUMENT.withDescription(
                                        "unary request is missing"
                                ),
                                new Metadata()
                        );
                        handle.close();
                        selection.close();
                        return;
                    }
                    clientCall.sendMessage(request);
                    clientCall.halfClose();
                }

                @Override
                public void onCancel() {
                    clientCall.cancel("consumer cancelled", null);
                    handle.close();
                    selection.close();
                }

                @Override
                public void onReady() {
                }
            };
            serverCall.request(1);
            return listener;
        };
    }

    private CallOptions callOptions(Duration routeTimeout) {
        long remainingNanos = maximumTimeout.toNanos();
        Deadline inbound = Context.current().getDeadline();
        if (inbound != null) {
            remainingNanos = Math.min(
                    remainingNanos,
                    inbound.timeRemaining(TimeUnit.NANOSECONDS)
            );
        }
        remainingNanos = Math.min(remainingNanos, routeTimeout.toNanos());
        if (remainingNanos <= 0) {
            return CallOptions.DEFAULT.withDeadlineAfter(
                    1,
                    TimeUnit.NANOSECONDS
            );
        }
        return CallOptions.DEFAULT.withDeadlineAfter(
                remainingNanos,
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
            String traceId) {
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
}
