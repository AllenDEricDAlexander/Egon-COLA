package top.egon.cola.component.rpc.contract;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.lang.reflect.Method;

public record RpcMethodDescriptor(
        Method javaMethod,
        String methodName,
        String fullMethodName,
        boolean idempotent,
        io.grpc.MethodDescriptor<Message, Message> grpcMethod,
        Descriptors.MethodDescriptor protoMethod
) {
}
