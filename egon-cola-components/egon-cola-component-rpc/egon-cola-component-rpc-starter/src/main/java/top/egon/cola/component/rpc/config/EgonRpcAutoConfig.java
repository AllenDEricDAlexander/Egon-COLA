package top.egon.cola.component.rpc.config;

import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProcessIdentityFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentityProvider;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.DefaultRpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.consumer.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.GatewayRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.consumer.RpcGatewayDirectory;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseManager;
import top.egon.cola.component.rpc.provider.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.RpcProviderExceptionMapper;
import top.egon.cola.component.rpc.provider.RpcProviderMetadataContributor;
import top.egon.cola.component.rpc.provider.RpcProviderMetadataMerger;
import top.egon.cola.component.rpc.provider.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.RpcServerServiceDefinitionFactory;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(EgonRpcProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.rpc",
        name = "enabled",
        havingValue = "true"
)
public class EgonRpcAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public RpcContractValidator rpcContractValidator() {
        return new RpcContractValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcProcessIdentityProvider rpcProcessIdentityProvider(
            Environment environment,
            EgonRpcProperties properties) {
        RpcProcessIdentityFactory factory =
                new RpcProcessIdentityFactory(environment, properties);
        return factory::create;
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcProcessIdentity rpcProcessIdentity(
            RpcProcessIdentityProvider identityProvider) {
        return identityProvider.identity();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.provider",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean
    public RpcProviderMethodRegistry rpcProviderMethodRegistry(
            ApplicationContext applicationContext,
            RpcContractValidator contractValidator) {
        return new RpcProviderBeanScanner(
                applicationContext,
                contractValidator
        ).scan();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.provider",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean
    public RpcContractCatalog rpcContractCatalog(
            RpcProviderMethodRegistry methodRegistry) {
        return new DefaultRpcContractCatalog(methodRegistry);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.provider",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean
    public RpcProviderMetadataMerger rpcProviderMetadataMerger(
            ObjectProvider<RpcProviderMetadataContributor> contributors) {
        return new RpcProviderMetadataMerger(
                contributors.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.provider",
            name = "enabled",
            havingValue = "true"
    )
    public RpcProviderLifecycle rpcProviderLifecycle(
            RpcProviderMethodRegistry methodRegistry,
            RpcProviderMetadataMerger metadataMerger,
            ObjectProvider<RpcProviderRegistry> registryProvider,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            Environment environment,
            ObjectProvider<ServerInterceptor> serverInterceptors,
            ObjectProvider<RpcProviderExceptionMapper> exceptionMappers) {
        String runtimeVersion = environment.getProperty(
                "egon.rpc.runtime-version"
        );
        if (runtimeVersion == null || runtimeVersion.isBlank()) {
            runtimeVersion = RpcRuntimeVersion.load();
        }
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProviderRegistry registry = registryProvider.getIfAvailable();
        RpcProviderLeaseManager leaseManager = registry == null
                ? null
                : new RpcProviderLeaseManager(
                        registry,
                        availability,
                        properties,
                        processIdentity,
                        runtimeVersion,
                        metadataMerger
                );
        List<ServerInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new RpcProviderServerInterceptor());
        serverInterceptors.orderedStream().forEach(interceptors::add);
        return new RpcProviderLifecycle(
                methodRegistry,
                new RpcServerServiceDefinitionFactory(
                        availability,
                        exceptionMappers.orderedStream().toList()
                ),
                new RpcProviderServerFactory(transportSecurity(properties)),
                leaseManager,
                availability,
                interceptors,
                properties,
                processIdentity
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    public RpcConsumerGatewayManager rpcConsumerGatewayManager(
            RpcGatewayDirectory gatewayDirectory,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity) {
        return new RpcConsumerGatewayManager(
                gatewayDirectory,
                new RpcConsumerChannelFactory(transportSecurity(properties)),
                properties,
                processIdentity
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    public GatewayRpcInvocationChannelProvider
            gatewayRpcInvocationChannelProvider(
            RpcConsumerGatewayManager gatewayManager) {
        return new GatewayRpcInvocationChannelProvider(gatewayManager);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    public RpcConsumerProxyFactory rpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            GatewayRpcInvocationChannelProvider channelProvider,
            RpcProcessIdentity processIdentity,
            EgonRpcProperties properties) {
        return new RpcConsumerProxyFactory(
                contractValidator,
                channelProvider,
                processIdentity,
                new RpcStatusExceptionMapper(),
                properties.getConsumer().getDefaultTimeoutMs()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    public EgonRpcReferenceBeanPostProcessor egonRpcReferenceBeanPostProcessor(
            RpcConsumerProxyFactory proxyFactory) {
        return new EgonRpcReferenceBeanPostProcessor(proxyFactory);
    }

    private RpcTransportSecurity transportSecurity(
            EgonRpcProperties properties) {
        EgonRpcProperties.Tls tls = properties.getTls();
        return new RpcTransportSecurity(
                tls.isEnabled(),
                tls.isDevelopmentPlaintext(),
                tls.getCertificateChainPath(),
                tls.getPrivateKeyPath(),
                tls.getTrustCertificateCollectionPath()
        );
    }
}
