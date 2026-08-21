package top.egon.cola.component.rpc.contract.descriptor;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationMode;

import java.lang.reflect.Method;

public record RpcMethodDescriptor(
        Method javaMethod,
        String methodName,
        String fullMethodName,
        boolean idempotent,
        RpcInvocationMode invocationMode,
        Class<? extends Message> requestType,
        Class<? extends Message> responseType,
        io.grpc.MethodDescriptor<Message, Message> grpcMethod,
        Descriptors.MethodDescriptor protoMethod
) {

    public RpcMethodDescriptor(
            Method javaMethod,
            String methodName,
            String fullMethodName,
            boolean idempotent,
            io.grpc.MethodDescriptor<Message, Message> grpcMethod,
            Descriptors.MethodDescriptor protoMethod) {
        this(
                javaMethod,
                methodName,
                fullMethodName,
                idempotent,
                RpcInvocationMode.BLOCKING,
                messageType(javaMethod.getParameterTypes()[0]),
                messageType(javaMethod.getReturnType()),
                grpcMethod,
                protoMethod);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Message> messageType(Class<?> type) {
        return (Class<? extends Message>) type;
    }
}
