package top.egon.cola.component.rpc.consumer;

import top.egon.cola.component.rpc.context.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.util.List;
import java.util.function.Function;

/**
 * 创建不经过 Gateway、且显式拥有 Channel 生命周期的 RPC 客户端。
 *
 * <p>Creates RPC clients that bypass the Gateway and explicitly own their
 * channel lifecycle.
 */
public final class RpcDirectClientFactory {

    private final RpcContractValidator contractValidator;

    private final RpcStatusExceptionMapper statusMapper;

    private final Function<RpcDirectClientSettings,
            DirectRpcInvocationChannelProvider> channelProviderFactory;

    /**
     * 使用默认契约校验器和状态映射器创建工厂。
     *
     * <p>Creates a factory with the default contract validator and status
     * mapper.
     */
    public RpcDirectClientFactory() {
        this(
                new RpcContractValidator(),
                new RpcStatusExceptionMapper(),
                DirectRpcInvocationChannelProvider::new
        );
    }

    /**
     * 使用显式核心组件创建工厂。
     *
     * <p>Creates a factory with explicit core components.
     *
     * @param contractValidator 契约校验器 / contract validator
     * @param statusMapper 状态异常映射器 / status exception mapper
     */
    public RpcDirectClientFactory(
            RpcContractValidator contractValidator,
            RpcStatusExceptionMapper statusMapper) {
        this(
                contractValidator,
                statusMapper,
                DirectRpcInvocationChannelProvider::new
        );
    }

    RpcDirectClientFactory(
            RpcContractValidator contractValidator,
            RpcStatusExceptionMapper statusMapper,
            Function<RpcDirectClientSettings,
                    DirectRpcInvocationChannelProvider>
                    channelProviderFactory) {
        this.contractValidator = contractValidator;
        this.statusMapper = statusMapper;
        this.channelProviderFactory = channelProviderFactory;
    }

    /**
     * 创建类型化直连客户端句柄。
     *
     * <p>Creates a typed direct client handle.
     *
     * @param contractType RPC 契约类型 / RPC contract type
     * @param settings 直连参数 / direct settings
     * @param interceptorFactories 有序请求拦截器工厂 / ordered factories
     * @param <T> RPC 契约类型 / RPC contract type
     * @return 拥有 Channel 的客户端句柄 / channel-owning client handle
     */
    public <T> RpcDirectClientHandle<T> create(
            Class<T> contractType,
            RpcDirectClientSettings settings,
            List<RpcClientInterceptorFactory> interceptorFactories) {
        DirectRpcInvocationChannelProvider channelProvider =
                channelProviderFactory.apply(settings);
        try {
            RpcConsumerProxyFactory proxyFactory =
                    new RpcConsumerProxyFactory(
                            contractValidator,
                            channelProvider,
                            settings.processIdentity(),
                            statusMapper,
                            settings.deadlineMs(),
                            interceptorFactories
                    );
            return new RpcDirectClientHandle<>(
                    proxyFactory.create(contractType, settings.deadlineMs()),
                    channelProvider
            );
        } catch (RuntimeException exception) {
            channelProvider.close();
            throw exception;
        }
    }
}
