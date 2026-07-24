package top.egon.cola.component.rpc.consumer;

import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import top.egon.cola.component.rpc.context.RpcConsumerClientInterceptor;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcMethodDescriptor;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
        Channel gatewayChannel = ClientInterceptors.intercept(
                gatewayManager.currentChannel(),
                new RpcConsumerClientInterceptor(contract, processIdentity)
        );
        CallOptions callOptions = CallOptions.DEFAULT.withDeadlineAfter(
                timeoutMs,
                TimeUnit.MILLISECONDS
        );
        try {
            return ClientCalls.blockingUnaryCall(
                    gatewayChannel,
                    rpcMethod.grpcMethod(),
                    callOptions,
                    (Message) args[0]
            );
        } catch (StatusRuntimeException exception) {
            throw statusMapper.map(exception);
        }
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
