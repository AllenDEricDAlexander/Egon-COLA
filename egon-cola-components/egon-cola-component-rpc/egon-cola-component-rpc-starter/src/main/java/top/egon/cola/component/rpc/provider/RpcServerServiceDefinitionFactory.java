package top.egon.cola.component.rpc.provider;

import com.google.protobuf.Message;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.rpc.context.RpcInvocationMetadata;
import top.egon.cola.component.rpc.exception.EgonRpcRejectedException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class RpcServerServiceDefinitionFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RpcServerServiceDefinitionFactory.class);

    private final RpcProviderAvailabilityRegistry availability;

    private final List<RpcProviderExceptionMapper> exceptionMappers;

    public RpcServerServiceDefinitionFactory(
            RpcProviderAvailabilityRegistry availability) {
        this(availability, List.of());
    }

    public RpcServerServiceDefinitionFactory(
            RpcProviderAvailabilityRegistry availability,
            List<RpcProviderExceptionMapper> exceptionMappers) {
        this.availability = availability;
        this.exceptionMappers = List.copyOf(exceptionMappers);
    }

    public List<ServerServiceDefinition> create(
            RpcProviderMethodRegistry registry) {
        return registry.providers().stream()
                .map(provider -> createService(registry, provider))
                .toList();
    }

    private ServerServiceDefinition createService(
            RpcProviderMethodRegistry registry,
            RpcProviderBinding provider) {
        RpcServiceIdentity service = provider.serviceIdentity();
        ServerServiceDefinition.Builder builder =
                ServerServiceDefinition.builder(service.serviceName());
        for (RpcProviderMethodBinding binding : registry.methods(service)) {
            ServerCalls.UnaryMethod<Message, Message> handler =
                    (request, observer) -> invoke(binding, request, observer);
            builder.addMethod(ServerMethodDefinition.create(
                    binding.method().grpcMethod(),
                    ServerCalls.asyncUnaryCall(handler)
            ));
        }
        return builder.build();
    }

    private void invoke(RpcProviderMethodBinding binding,
                        Message request,
                        StreamObserver<Message> observer) {
        RpcServiceIdentity service = binding.provider().serviceIdentity();
        if (!availability.isAvailable(service)) {
            observer.onError(Status.UNAVAILABLE
                    .withDescription("RPC provider is unavailable")
                    .asRuntimeException());
            return;
        }
        if (request == null) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription("RPC request is required")
                    .asRuntimeException());
            return;
        }
        try {
            Object response = binding.method().javaMethod().invoke(
                    binding.provider().bean(),
                    request
            );
            if (!(response instanceof Message message)) {
                throw new IllegalStateException(
                        "RPC provider returned no Protobuf response"
                );
            }
            observer.onNext(message);
            observer.onCompleted();
        } catch (InvocationTargetException exception) {
            fail(binding, observer, exception.getTargetException());
        } catch (Throwable throwable) {
            fail(binding, observer, throwable);
        }
    }

    private void fail(
            RpcProviderMethodBinding binding,
            StreamObserver<Message> observer,
            Throwable throwable) {
        for (RpcProviderExceptionMapper mapper : exceptionMappers) {
            try {
                var mapped = mapper.map(throwable);
                if (mapped.isPresent()) {
                    observer.onError(mapped.get());
                    return;
                }
            } catch (RuntimeException mapperFailure) {
                LOGGER.error(
                        "RPC Provider exception mapper failed mapper={}",
                        mapper.getClass().getName(),
                        mapperFailure
                );
            }
        }
        if (throwable instanceof EgonRpcRejectedException) {
            observer.onError(Status.PERMISSION_DENIED
                    .withDescription("RPC provider rejected the request")
                    .asRuntimeException());
            return;
        }
        RpcInvocationMetadata invocation = RpcInvocationMetadata.current();
        LOGGER.error(
                "RPC Provider invocation failed service={} method={} "
                        + "invocationId={}",
                binding.provider().serviceIdentity(),
                binding.method().methodName(),
                invocation == null ? null : invocation.invocationId(),
                throwable
        );
        observer.onError(Status.INTERNAL
                .withDescription("RPC provider invocation failed")
                .asRuntimeException());
    }
}
