package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MockDynamicHandlerRegistry extends HandlerRegistry {

    private final Map<String, ServerMethodDefinition<?, ?>> methods =
            new ConcurrentHashMap<>();

    void register(
            String fullMethodName,
            ServerCalls.UnaryMethod<byte[], byte[]> handler) {
        int separator = fullMethodName.lastIndexOf('/');
        if (separator <= 0 || separator == fullMethodName.length() - 1) {
            throw new IllegalArgumentException("invalid full RPC method name");
        }
        MethodDescriptor<byte[], byte[]> descriptor =
                MethodDescriptor.<byte[], byte[]>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName(fullMethodName)
                        .setRequestMarshaller(new MockByteArrayMarshaller())
                        .setResponseMarshaller(new MockByteArrayMarshaller())
                        .build();
        ServerMethodDefinition<byte[], byte[]> definition =
                ServerMethodDefinition.create(
                        descriptor,
                        ServerCalls.asyncUnaryCall(handler)
                );
        if (methods.putIfAbsent(fullMethodName, definition) != null) {
            throw new IllegalArgumentException(
                    "duplicate mock gateway method"
            );
        }
    }

    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(
            String methodName,
            String authority) {
        return methods.get(methodName);
    }

    @Override
    public List<ServerServiceDefinition> getServices() {
        return List.of();
    }
}
