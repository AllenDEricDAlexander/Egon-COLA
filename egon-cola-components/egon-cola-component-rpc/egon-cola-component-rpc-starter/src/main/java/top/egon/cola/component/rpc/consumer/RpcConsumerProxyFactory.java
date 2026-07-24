package top.egon.cola.component.rpc.consumer;

import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.lang.reflect.Proxy;

public class RpcConsumerProxyFactory {

    private final RpcContractValidator contractValidator;

    private final RpcConsumerGatewayManager gatewayManager;

    private final RpcProcessIdentity processIdentity;

    private final RpcStatusExceptionMapper statusMapper;

    private final long defaultTimeoutMs;

    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcConsumerGatewayManager gatewayManager,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs) {
        this.contractValidator = contractValidator;
        this.gatewayManager = gatewayManager;
        this.processIdentity = processIdentity;
        this.statusMapper = statusMapper;
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public <T> T create(Class<T> contractType, long referenceTimeoutMs) {
        RpcContractDescriptor contract =
                contractValidator.validate(contractType);
        long timeoutMs = referenceTimeoutMs > 0
                ? Math.min(referenceTimeoutMs, defaultTimeoutMs)
                : defaultTimeoutMs;
        Object proxy = Proxy.newProxyInstance(
                contractType.getClassLoader(),
                new Class<?>[]{contractType},
                new RpcConsumerInvocationHandler(
                        contract,
                        gatewayManager,
                        processIdentity,
                        statusMapper,
                        timeoutMs
                )
        );
        return contractType.cast(proxy);
    }
}
