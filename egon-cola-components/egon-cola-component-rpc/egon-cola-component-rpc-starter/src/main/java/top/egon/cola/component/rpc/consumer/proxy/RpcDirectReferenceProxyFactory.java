package top.egon.cola.component.rpc.consumer.proxy;

import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.provider.ProviderRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.util.List;

/**
 * 根据 Direct 注解创建绑定到精确 Provider query 的类型化 RPC 代理。
 *
 * <p>Creates typed RPC proxies bound to the exact Provider query declared by
 * a Direct reference.
 */
public class RpcDirectReferenceProxyFactory {

    private final RpcContractValidator contractValidator;

    private final RpcConsumerProviderManager providerManager;

    private final RpcProcessIdentity processIdentity;

    private final RpcStatusExceptionMapper statusMapper;

    private final long defaultTimeoutMs;

    private final List<RpcClientInterceptorFactory> interceptorFactories;

    public RpcDirectReferenceProxyFactory(
            RpcContractValidator contractValidator,
            RpcConsumerProviderManager providerManager,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs,
            List<RpcClientInterceptorFactory> interceptorFactories) {
        this.contractValidator = contractValidator;
        this.providerManager = providerManager;
        this.processIdentity = processIdentity;
        this.statusMapper = statusMapper;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.interceptorFactories = List.copyOf(interceptorFactories);
    }

    public <T> T create(
            Class<T> contractType,
            EgonRpcDirectReference reference) {
        RpcContractDescriptor contract =
                contractValidator.validate(contractType);
        RpcProviderQuery query = new RpcProviderQuery(
                reference.bizCode(),
                reference.appCode(),
                defaultIfBlank(reference.env(), processIdentity.env()),
                contract.serviceName(),
                defaultIfBlank(reference.group(), contract.group()),
                defaultIfBlank(reference.version(), contract.version()),
                "grpc"
        );
        ProviderRpcInvocationChannelProvider channelProvider =
                providerManager.register(query);
        RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                contractValidator,
                channelProvider,
                processIdentity,
                statusMapper,
                defaultTimeoutMs,
                interceptorFactories
        );
        return proxyFactory.create(contractType, reference.timeoutMs());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
