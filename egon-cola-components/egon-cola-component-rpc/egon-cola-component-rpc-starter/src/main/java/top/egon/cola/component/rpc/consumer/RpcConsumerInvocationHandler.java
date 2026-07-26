package top.egon.cola.component.rpc.consumer;

import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import top.egon.cola.component.rpc.context.RpcConsumerClientInterceptor;
import top.egon.cola.component.rpc.context.RpcFailureStage;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcMethodDescriptor;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RpcConsumerInvocationHandler implements InvocationHandler {

    private final RpcContractDescriptor contract;

    private final RpcConsumerGatewayManager gatewayManager;

    private final RpcProcessIdentity processIdentity;

    private final RpcStatusExceptionMapper statusMapper;

    private final long timeoutMs;

    public RpcConsumerInvocationHandler(
            RpcContractDescriptor contract,
            RpcConsumerGatewayManager gatewayManager,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long timeoutMs) {
        this.contract = contract;
        this.gatewayManager = gatewayManager;
        this.processIdentity = processIdentity;
        this.statusMapper = statusMapper;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args);
        }
        if (args == null || args.length != 1 || !(args[0] instanceof Message)) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_REQUEST,
                    "RPC request must be one Protobuf Message"
            );
        }
        RpcMethodDescriptor rpcMethod = contract.method(method);
        var interceptor =
                new RpcConsumerClientInterceptor(contract, processIdentity);
        Set<ManagedChannel> attempted = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );
        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        StatusRuntimeException lastFailure = null;
        for (int attempt = 0;
             attempt < gatewayManager.maxAttempts();
             attempt++) {
            ManagedChannel managedChannel =
                    gatewayManager.currentChannel(attempted);
            attempted.add(managedChannel);
            Channel gatewayChannel = ClientInterceptors.intercept(
                    managedChannel,
                    interceptor
            );
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                break;
            }
            CallOptions callOptions = CallOptions.DEFAULT.withDeadlineAfter(
                    remainingNanos,
                    TimeUnit.NANOSECONDS
            );
            try {
                return ClientCalls.blockingUnaryCall(
                        gatewayChannel,
                        rpcMethod.grpcMethod(),
                        callOptions,
                        (Message) args[0]
                );
            } catch (StatusRuntimeException exception) {
                lastFailure = exception;
                if (!retryableGatewayFailure(rpcMethod, exception)
                        || attempt + 1 >= gatewayManager.maxAttempts()) {
                    throw statusMapper.map(exception);
                }
                gatewayManager.recordFailure(managedChannel);
            }
        }
        throw lastFailure == null
                ? new EgonRpcException(
                EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED,
                "RPC deadline exceeded"
        )
                : statusMapper.map(lastFailure);
    }

    private boolean retryableGatewayFailure(
            RpcMethodDescriptor method,
            StatusRuntimeException exception) {
        return method.idempotent()
                && exception.getStatus().getCode() == Status.Code.UNAVAILABLE
                && RpcFailureStage.from(exception.getTrailers())
                .filter(stage -> stage == RpcFailureStage.GATEWAY)
                .isPresent();
    }

    private Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "EgonRpcProxy[" + contract.contractType().getName() + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            default -> throw new IllegalStateException(
                    "unsupported Object method: " + method
            );
        };
    }
}
