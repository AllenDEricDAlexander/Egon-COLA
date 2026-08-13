package top.egon.cola.component.rpc.contract.validation;

import com.google.protobuf.Message;
import io.grpc.MethodDescriptor.MethodType;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.contract.descriptor.GeneratedGrpcDescriptorResolver;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RpcContractValidator {

    private final GeneratedGrpcDescriptorResolver descriptorResolver;

    private final ConcurrentMap<Class<?>, RpcContractDescriptor> cache =
            new ConcurrentHashMap<>();

    public RpcContractValidator() {
        this(new GeneratedGrpcDescriptorResolver());
    }

    public RpcContractValidator(
            GeneratedGrpcDescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    public RpcContractDescriptor validate(Class<?> contractType) {
        RpcContractDescriptor cached = cache.get(contractType);
        if (cached != null) {
            return cached;
        }
        RpcContractDescriptor validated = validateUncached(contractType);
        RpcContractDescriptor concurrent =
                cache.putIfAbsent(contractType, validated);
        return concurrent == null ? validated : concurrent;
    }

    private RpcContractDescriptor validateUncached(Class<?> contractType) {
        if (contractType == null || !contractType.isInterface()) {
            throw invalid("RPC contract must be an interface");
        }
        EgonRpcService service =
                contractType.getAnnotation(EgonRpcService.class);
        if (service == null) {
            throw invalid("RPC contract is missing @EgonRpcService");
        }
        requireText(service.group(), "RPC group");
        requireText(service.version(), "RPC version");
        GeneratedGrpcDescriptorResolver.ResolvedGrpcService generated =
                descriptorResolver.resolve(service.grpcClass());
        List<Method> javaMethods = Arrays.stream(contractType.getMethods())
                .filter(method -> method.getDeclaringClass() != Object.class)
                .toList();
        rejectOverloads(javaMethods);

        List<RpcMethodDescriptor> methods = new ArrayList<>();
        for (Method javaMethod : javaMethods) {
            methods.add(validateMethod(javaMethod, generated));
        }
        if (methods.isEmpty()) {
            throw invalid("RPC contract has no methods");
        }
        return new RpcContractDescriptor(
                contractType,
                generated.serviceDescriptor().getName(),
                service.group(),
                service.version(),
                methods
        );
    }

    private RpcMethodDescriptor validateMethod(
            Method javaMethod,
            GeneratedGrpcDescriptorResolver.ResolvedGrpcService generated) {
        EgonRpcMethod rpcMethod = javaMethod.getAnnotation(EgonRpcMethod.class);
        if (rpcMethod == null) {
            throw invalid("RPC method is missing @EgonRpcMethod");
        }
        requireText(rpcMethod.name(), "RPC method name");
        if (javaMethod.getParameterCount() != 1) {
            throw invalid("RPC method must declare exactly one request parameter");
        }
        Class<?> requestType = javaMethod.getParameterTypes()[0];
        Class<?> responseType = javaMethod.getReturnType();
        if (!Message.class.isAssignableFrom(requestType)
                || !Message.class.isAssignableFrom(responseType)) {
            throw invalid("RPC request and response must implement Protobuf Message");
        }
        GeneratedGrpcDescriptorResolver.ResolvedGrpcMethod generatedMethod =
                generated.methods().get(rpcMethod.name());
        if (generatedMethod == null) {
            throw invalid("RPC method does not exist in the generated Proto service");
        }
        if (generatedMethod.grpcMethod().getType() != MethodType.UNARY
                || generatedMethod.protoMethod().isClientStreaming()
                || generatedMethod.protoMethod().isServerStreaming()) {
            throw invalid("RPC V1 supports unary methods only");
        }
        Message request = defaultInstance(requestType);
        Message response = defaultInstance(responseType);
        if (!request.getDescriptorForType().equals(
                generatedMethod.protoMethod().getInputType())) {
            throw invalid("RPC request type does not match the Proto input");
        }
        if (!response.getDescriptorForType().equals(
                generatedMethod.protoMethod().getOutputType())) {
            throw invalid("RPC response type does not match the Proto output");
        }
        @SuppressWarnings("unchecked")
        io.grpc.MethodDescriptor<Message, Message> grpcMethod =
                (io.grpc.MethodDescriptor<Message, Message>)
                        generatedMethod.grpcMethod();
        return new RpcMethodDescriptor(
                javaMethod,
                rpcMethod.name(),
                grpcMethod.getFullMethodName(),
                rpcMethod.idempotent(),
                grpcMethod,
                generatedMethod.protoMethod()
        );
    }

    private Message defaultInstance(Class<?> messageType) {
        try {
            Method accessor = messageType.getMethod("getDefaultInstance");
            Object value = accessor.invoke(null);
            if (!(value instanceof Message message)) {
                throw invalid("Protobuf type has no valid default instance");
            }
            return message;
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw invalid("Protobuf type has no public default instance", exception);
        } catch (InvocationTargetException exception) {
            throw invalid(
                    "Protobuf default instance invocation failed",
                    exception.getTargetException()
            );
        }
    }

    private void rejectOverloads(List<Method> methods) {
        Set<String> names = new HashSet<>();
        for (Method method : methods) {
            if (!names.add(method.getName())) {
                throw invalid("RPC contract does not support overloaded methods");
            }
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName + " is required");
        }
    }

    private EgonRpcException invalid(String message) {
        return new EgonRpcException(
                EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                message
        );
    }

    private EgonRpcException invalid(String message, Throwable cause) {
        return new EgonRpcException(
                EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                message,
                cause
        );
    }
}
