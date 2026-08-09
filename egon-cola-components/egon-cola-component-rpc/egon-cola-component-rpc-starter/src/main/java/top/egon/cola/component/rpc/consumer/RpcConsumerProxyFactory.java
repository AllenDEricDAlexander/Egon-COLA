package top.egon.cola.component.rpc.consumer;

import top.egon.cola.component.rpc.context.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 根据已校验契约和调用 Channel Strategy 创建类型化 RPC 代理。
 *
 * <p>Creates typed RPC proxies from validated contracts and an invocation
 * channel strategy.
 */
public class RpcConsumerProxyFactory {

    private final RpcContractValidator contractValidator;

    private final RpcInvocationChannelProvider channelProvider;

    private final RpcProcessIdentity processIdentity;

    private final RpcStatusExceptionMapper statusMapper;

    private final long defaultTimeoutMs;

    private final List<RpcClientInterceptorFactory> interceptorFactories;

    /**
     * 创建不包含额外请求拦截器的代理工厂。
     *
     * <p>Creates a proxy factory without additional request interceptors.
     *
     * @param contractValidator 契约校验器 / contract validator
     * @param channelProvider Channel Strategy / channel strategy
     * @param processIdentity 调用方身份 / caller identity
     * @param statusMapper 状态映射器 / status mapper
     * @param defaultTimeoutMs 默认期限 / default deadline
     */
    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcInvocationChannelProvider channelProvider,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs) {
        this(
                contractValidator,
                channelProvider,
                processIdentity,
                statusMapper,
                defaultTimeoutMs,
                List.of()
        );
    }

    /**
     * 创建包含有序请求感知拦截器工厂的代理工厂。
     *
     * <p>Creates a proxy factory with ordered request-aware interceptor
     * factories.
     *
     * @param contractValidator 契约校验器 / contract validator
     * @param channelProvider Channel Strategy / channel strategy
     * @param processIdentity 调用方身份 / caller identity
     * @param statusMapper 状态映射器 / status mapper
     * @param defaultTimeoutMs 默认期限 / default deadline
     * @param interceptorFactories 有序拦截器工厂 / ordered factories
     */
    public RpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcInvocationChannelProvider channelProvider,
            RpcProcessIdentity processIdentity,
            RpcStatusExceptionMapper statusMapper,
            long defaultTimeoutMs,
            List<RpcClientInterceptorFactory> interceptorFactories) {
        this.contractValidator = contractValidator;
        this.channelProvider = channelProvider;
        this.processIdentity = processIdentity;
        this.statusMapper = statusMapper;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.interceptorFactories = List.copyOf(interceptorFactories);
    }

    /**
     * 为指定契约创建代理，并将引用期限限制在全局默认期限以内。
     *
     * <p>Creates a contract proxy while capping its reference deadline at the
     * configured default.
     *
     * @param contractType 契约类型 / contract type
     * @param referenceTimeoutMs 引用期限 / reference deadline
     * @param <T> 契约类型 / contract type
     * @return 类型化代理 / typed proxy
     */
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
                        channelProvider,
                        processIdentity,
                        statusMapper,
                        timeoutMs,
                        interceptorFactories
                )
        );
        return contractType.cast(proxy);
    }
}
