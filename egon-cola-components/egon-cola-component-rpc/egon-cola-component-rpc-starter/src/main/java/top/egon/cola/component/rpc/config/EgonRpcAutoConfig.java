package top.egon.cola.component.rpc.config;

import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentityFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentityProvider;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.catalog.DefaultRpcContractCatalog;
import top.egon.cola.component.rpc.contract.catalog.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.consumer.proxy.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.gateway.GatewayRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderDirectory;
import top.egon.cola.component.rpc.consumer.proxy.RpcDirectReferenceProxyFactory;
import top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayDirectory;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseManager;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataContributor;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataMerger;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistry;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;

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
    @ConditionalOnMissingBean
    public RpcConsumerChannelFactory rpcConsumerChannelFactory(
            EgonRpcProperties properties) {
        properties.getConsumer().validateSharedSettings();
        return new RpcConsumerChannelFactory(transportSecurity(properties));
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnBean(RpcGatewayDirectory.class)
    public RpcConsumerGatewayManager rpcConsumerGatewayManager(
            RpcGatewayDirectory gatewayDirectory,
            RpcConsumerChannelFactory channelFactory,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity) {
        return new RpcConsumerGatewayManager(
                gatewayDirectory,
                channelFactory,
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
    @ConditionalOnBean(RpcConsumerGatewayManager.class)
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
    @ConditionalOnBean(GatewayRpcInvocationChannelProvider.class)
    public RpcConsumerProxyFactory rpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            GatewayRpcInvocationChannelProvider channelProvider,
            RpcProcessIdentity processIdentity,
            EgonRpcProperties properties,
            ObjectProvider<RpcClientInterceptorFactory>
                    interceptorFactories) {
        return new RpcConsumerProxyFactory(
                contractValidator,
                channelProvider,
                processIdentity,
                new RpcStatusExceptionMapper(),
                properties.getConsumer().getDefaultTimeoutMs(),
                interceptorFactories.orderedStream().toList()
        );
    }

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
    @ConditionalOnBean(RpcProviderDirectory.class)
    public RpcConsumerProviderManager rpcConsumerProviderManager(
            RpcProviderDirectory providerDirectory,
            RpcConsumerChannelFactory channelFactory,
            EgonRpcProperties properties) {
        return new RpcConsumerProviderManager(
                providerDirectory,
                channelFactory,
                properties
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnBean(RpcConsumerProviderManager.class)
    public RpcDirectReferenceProxyFactory rpcDirectReferenceProxyFactory(
            RpcContractValidator contractValidator,
            RpcConsumerProviderManager providerManager,
            RpcProcessIdentity processIdentity,
            EgonRpcProperties properties,
            ObjectProvider<RpcClientInterceptorFactory>
                    interceptorFactories) {
        return new RpcDirectReferenceProxyFactory(
                contractValidator,
                providerManager,
                processIdentity,
                new RpcStatusExceptionMapper(),
                properties.getConsumer().getDefaultTimeoutMs(),
                interceptorFactories.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.rpc.consumer",
            name = "enabled",
            havingValue = "true"
    )
    public EgonRpcReferenceBeanPostProcessor egonRpcReferenceBeanPostProcessor(
            ObjectProvider<RpcConsumerProxyFactory> gatewayProxyFactory,
            ObjectProvider<RpcConsumerGatewayManager> gatewayManager,
            ObjectProvider<RpcDirectReferenceProxyFactory>
                    directProxyFactory) {
        return new EgonRpcReferenceBeanPostProcessor(
                gatewayProxyFactory.getIfAvailable(),
                gatewayManager.getIfAvailable(),
                directProxyFactory.getIfAvailable()
        );
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
