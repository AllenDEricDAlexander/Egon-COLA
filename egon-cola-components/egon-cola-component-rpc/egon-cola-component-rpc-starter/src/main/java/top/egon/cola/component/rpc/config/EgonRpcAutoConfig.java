package top.egon.cola.component.rpc.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProcessIdentityFactory;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.DefaultRpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.consumer.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseManager;
import top.egon.cola.component.rpc.provider.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.RpcServerServiceDefinitionFactory;

@AutoConfiguration
@EnableConfigurationProperties({EgonRpcProperties.class, DdcProperties.class})
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
    public RpcProcessIdentity rpcProcessIdentity(
            Environment environment,
            DdcProperties ddcProperties) {
        return new RpcProcessIdentityFactory(
                environment,
                ddcProperties
        ).create();
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
    public RpcProviderLifecycle rpcProviderLifecycle(
            RpcProviderMethodRegistry methodRegistry,
            DdcServiceRegistryClient registryClient,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            Environment environment) {
        String runtimeVersion = environment.getProperty(
                "egon.rpc.runtime-version"
        );
        if (runtimeVersion == null || runtimeVersion.isBlank()) {
            runtimeVersion = RpcRuntimeVersion.load();
        }
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProviderLeaseManager leaseManager = new RpcProviderLeaseManager(
                registryClient,
                availability,
                properties,
                processIdentity,
                runtimeVersion
        );
        return new RpcProviderLifecycle(
                methodRegistry,
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                leaseManager,
                availability,
                new RpcProviderServerInterceptor(),
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
            DdcServiceRegistryClient registryClient,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity) {
        return new RpcConsumerGatewayManager(
                registryClient,
                new RpcConsumerChannelFactory(),
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
    public RpcConsumerProxyFactory rpcConsumerProxyFactory(
            RpcContractValidator contractValidator,
            RpcConsumerGatewayManager gatewayManager,
            RpcProcessIdentity processIdentity,
            EgonRpcProperties properties) {
        return new RpcConsumerProxyFactory(
                contractValidator,
                gatewayManager,
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
}
