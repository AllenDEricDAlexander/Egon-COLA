package top.egon.cola.component.rpc.ddc.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.autoconfigure.DdcAutoConfiguration;
import top.egon.cola.component.ddc.autoconfigure.DdcRedisAutoConfiguration;
import top.egon.cola.component.ddc.autoconfigure.DdcRegistryAutoConfiguration;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.listener.registry.DdcRegistrySubscriptionCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.service.registry.DdcRegistrySnapshotLoader;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.consumer.RpcGatewayDirectory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProcessIdentityProvider;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.ddc.client.registry.RpcDdcServiceRegistryClient;
import top.egon.cola.component.rpc.ddc.registry.*;
import top.egon.cola.component.rpc.provider.RpcProviderRegistry;

import java.net.InetAddress;

/**
 * 在业务 DDC 自动装配之前提供 Direct RPC Port 适配器。
 * / Supplies Direct RPC Port adapters before the business DDC auto-configurations.
 */
@AutoConfiguration(
        after = DdcRedisAutoConfiguration.class,
        before = {
                DdcAutoConfiguration.class,
                DdcRegistryAutoConfiguration.class,
                EgonRpcAutoConfig.class
        }
)
@EnableConfigurationProperties({DdcRpcProperties.class, DdcProperties.class})
public class DdcRpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DdcRpcClientFactory ddcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            Environment environment) {
        return new DdcRpcClientFactory(
                rpcProperties,
                ddcProperties,
                directIdentity(ddcProperties, environment)
        );
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.registry",
            name = "enabled",
            havingValue = "true"
    )
    public RpcProcessIdentityProvider ddcRpcProcessIdentityProvider(
            ObjectProvider<DdcInstanceIdentity> identity,
            DdcProperties properties,
            Environment environment) {
        return new DdcRpcProcessIdentityProvider(identity, properties, environment);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean(
            value = DdcConfigClient.class,
            name = "ddcConfigRpcClientHandle"
    )
    public DdcRpcClientHandle<DdcConfigClient> ddcConfigRpcClientHandle(
            DdcRpcClientFactory factory) {
        return factory.configClient();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean(DdcConfigClient.class)
    public DdcConfigClient ddcRpcConfigClient(
            @Qualifier("ddcConfigRpcClientHandle")
            DdcRpcClientHandle<DdcConfigClient> handle) {
        return handle.client();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnBean(name = "ddcRedissonClient")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.registry",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean(
            value = DdcServiceRegistryClient.class,
            name = "ddcRegistryRpcClientHandle"
    )
    public DdcRpcClientHandle<DdcServiceRegistryClient>
            ddcRegistryRpcClientHandle(DdcRpcClientFactory factory) {
        return factory.registryClient();
    }

    @Bean
    @ConditionalOnBean(name = "ddcRegistryRpcClientHandle")
    @ConditionalOnMissingBean(DdcRegistrySnapshotLoader.class)
    public RpcDdcRegistrySnapshotLoader rpcDdcRegistrySnapshotLoader(
            @Qualifier("ddcRegistryRpcClientHandle")
            DdcRpcClientHandle<DdcServiceRegistryClient> handle) {
        return new RpcDdcRegistrySnapshotLoader(
                (RpcDdcServiceRegistryClient) handle.client()
        );
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnBean({RpcDdcRegistrySnapshotLoader.class, RedissonClient.class})
    public DdcRegistrySubscriptionCoordinator ddcRpcRegistrySubscriptions(
            RpcDdcRegistrySnapshotLoader loader,
            @Qualifier("ddcRedissonClient") RedissonClient redissonClient,
            DdcProperties properties) {
        return new DdcRegistrySubscriptionCoordinator(
                loader,
                redissonClient,
                properties.getRegistry().getReconcileIntervalSeconds()
        );
    }

    @Bean
    @ConditionalOnBean(DdcRegistrySubscriptionCoordinator.class)
    @ConditionalOnMissingBean(DdcServiceRegistryClient.class)
    public DdcServiceRegistryClient ddcRpcServiceRegistryClient(
            @Qualifier("ddcRegistryRpcClientHandle")
            DdcRpcClientHandle<DdcServiceRegistryClient> handle,
            DdcRegistrySubscriptionCoordinator subscriptions) {
        RpcDdcServiceRegistryClient client =
                (RpcDdcServiceRegistryClient) handle.client();
        client.subscriptions(new RpcDdcServiceRegistryClient.RegistrySubscriptions() {
            @Override
            public top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription subscribe(
                    top.egon.cola.component.ddc.model.registry.DdcServiceKey key,
                    java.util.function.Consumer<top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot> listener) {
                return subscriptions.subscribe(key, listener);
            }

            @Override
            public top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription subscribeServices(
                    top.egon.cola.component.ddc.model.registry.DdcServiceQuery query,
                    java.util.function.Consumer<top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot> listener) {
                return subscriptions.subscribeServices(query, listener);
            }
        });
        return client;
    }

    @Bean
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    @ConditionalOnMissingBean(RpcProviderRegistry.class)
    public RpcProviderRegistry ddcRpcProviderRegistry(
            DdcServiceRegistryClient client,
            DdcProperties properties) {
        return new DdcRpcProviderRegistry(
                client, properties.getBizCode(), properties.getAppCode());
    }

    @Bean
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    @ConditionalOnMissingBean(RpcGatewayDirectory.class)
    public RpcGatewayDirectory ddcRpcGatewayDirectory(
            DdcServiceRegistryClient client,
            DdcProperties properties) {
        return new DdcRpcGatewayDirectory(
                client, properties.getBizCode(), properties.getAppCode());
    }

    private RpcProcessIdentity directIdentity(
            DdcProperties properties,
            Environment environment) {
        String application = environment.getProperty(
                "spring.application.name", properties.getAppCode());
        String host = environment.getProperty(
                "egon.cola.component.rpc.identity.host", localHost());
        long pid = ProcessHandle.current().pid();
        String instanceId = environment.getProperty(
                "egon.cola.component.rpc.identity.instance-id",
                application + "-" + host + "-" + pid
        );
        return new RpcProcessIdentity(
                application, properties.getEnv(), host, pid, instanceId);
    }

    private String localHost() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception ignored) { return "127.0.0.1"; }
    }
}
