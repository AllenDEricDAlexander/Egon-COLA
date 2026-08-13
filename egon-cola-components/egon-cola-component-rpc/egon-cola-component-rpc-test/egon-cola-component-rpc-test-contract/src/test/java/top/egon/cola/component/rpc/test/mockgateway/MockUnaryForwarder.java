package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

final class MockUnaryForwarder {

    private final MockProviderChannelFactory channelFactory;

    MockUnaryForwarder(MockProviderChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    void forward(MockProviderEndpoint endpoint,
                 String fullMethodName,
                 byte[] payload,
                 StreamObserver<byte[]> responseObserver) {
        MethodDescriptor<byte[], byte[]> method =
                MethodDescriptor.<byte[], byte[]>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName(fullMethodName)
                        .setRequestMarshaller(new MockByteArrayMarshaller())
                        .setResponseMarshaller(new MockByteArrayMarshaller())
                        .build();
        CallOptions callOptions = CallOptions.DEFAULT;
        Deadline deadline = Context.current().getDeadline();
        if (deadline != null) {
            callOptions = callOptions.withDeadline(deadline);
        }
        ClientCall<byte[], byte[]> call = channelFactory.channel(endpoint)
                .newCall(method, callOptions);
        if (responseObserver instanceof ServerCallStreamObserver<byte[]> server) {
            server.setOnCancelHandler(() ->
                    call.cancel("consumer cancelled", null)
            );
        }
        call.start(new ClientCall.Listener<>() {

            @Override
            public void onMessage(byte[] message) {
                responseObserver.onNext(message);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (status.isOk()) {
                    responseObserver.onCompleted();
                } else {
                    Metadata forwardedTrailers = new Metadata();
                    if (trailers != null) {
                        forwardedTrailers.merge(trailers);
                    }
                    if (status.getCode() == Status.Code.UNAVAILABLE) {
                        forwardedTrailers.put(
                                RpcMetadataKeys.FAILURE_STAGE,
                                "provider"
                        );
                    }
                    responseObserver.onError(
                            status.asRuntimeException(forwardedTrailers)
                    );
                }
            }
        }, forwardedMetadata());
        call.request(1);
        call.sendMessage(payload);
        call.halfClose();
    }

    private Metadata forwardedMetadata() {
        Metadata source = MockGatewayCallContext.METADATA.get();
        Metadata target = new Metadata();
        if (source == null) {
            return target;
        }
        copy(source, target, RpcMetadataKeys.SERVICE);
        copy(source, target, RpcMetadataKeys.GROUP);
        copy(source, target, RpcMetadataKeys.VERSION);
        copy(source, target, RpcMetadataKeys.INVOCATION_ID);
        copy(source, target, RpcMetadataKeys.SOURCE_APP);
        copy(source, target, RpcMetadataKeys.SOURCE_INSTANCE);
        copy(source, target, RpcMetadataKeys.TRACEPARENT);
        copy(source, target, RpcMetadataKeys.TRACESTATE);
        copy(source, target, RpcMetadataKeys.REQUEST_ID);
        return target;
    }

    private void copy(Metadata source,
                      Metadata target,
                      Metadata.Key<String> key) {
        String value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }
}
