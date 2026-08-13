package top.egon.cola.component.rpc.contract.descriptor;

import com.google.protobuf.Descriptors;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class GeneratedGrpcDescriptorResolver {

    public ResolvedGrpcService resolve(Class<?> grpcClass) {
        try {
            java.lang.reflect.Method accessor =
                    grpcClass.getMethod("getServiceDescriptor");
            if (!Modifier.isStatic(accessor.getModifiers())
                    || !ServiceDescriptor.class.isAssignableFrom(accessor.getReturnType())) {
                throw invalid("generated gRPC descriptor accessor is invalid");
            }
            ServiceDescriptor serviceDescriptor =
                    (ServiceDescriptor) accessor.invoke(null);
            if (serviceDescriptor == null) {
                throw invalid("generated gRPC service descriptor is null");
            }
            Map<String, ResolvedGrpcMethod> methods = new LinkedHashMap<>();
            for (MethodDescriptor<?, ?> method : serviceDescriptor.getMethods()) {
                Object schemaDescriptor = method.getSchemaDescriptor();
                if (!(schemaDescriptor instanceof ProtoMethodDescriptorSupplier supplier)) {
                    throw invalid("gRPC method has no Protobuf descriptor supplier");
                }
                Descriptors.MethodDescriptor protoMethod =
                        supplier.getMethodDescriptor();
                if (protoMethod == null) {
                    throw invalid("gRPC method Protobuf descriptor is null");
                }
                methods.put(
                        protoMethod.getName(),
                        new ResolvedGrpcMethod(method, protoMethod)
                );
            }
            return new ResolvedGrpcService(serviceDescriptor, Map.copyOf(methods));
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw invalid("generated gRPC descriptor is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw invalid(
                    "generated gRPC descriptor invocation failed",
                    exception.getTargetException()
            );
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

    public record ResolvedGrpcService(
            ServiceDescriptor serviceDescriptor,
            Map<String, ResolvedGrpcMethod> methods
    ) {
    }

    public record ResolvedGrpcMethod(
            MethodDescriptor<?, ?> grpcMethod,
            Descriptors.MethodDescriptor protoMethod
    ) {
    }
}
