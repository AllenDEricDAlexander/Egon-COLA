package top.egon.cola.component.ddc.config;

import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.client.HttpDdcAdminClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.listener.DdcRedisChangeListener;
import top.egon.cola.component.ddc.listener.DdcRedisChangeSubscription;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.processor.DdcBeanPostProcessor;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.repository.DdcRedisConfigRepository;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;
import top.egon.cola.component.ddc.service.DdcInstanceIdentityFactory;
import top.egon.cola.component.ddc.service.DdcInstanceService;
import top.egon.cola.component.ddc.service.DdcInstanceMetadataContributor;
import top.egon.cola.component.ddc.service.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.service.DdcRuntimeCoordinator;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(DdcProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.ddc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DdcAutoConfig {

    @Bean
    public DdcValueConverter ddcValueConverter() {
        return new DdcValueConverter();
    }

    @Bean
    public DdcLocalConfigRepository ddcLocalConfigRepository() {
        return new DdcLocalConfigRepository();
    }

    @Bean
    public DdcFieldBindingService ddcFieldBindingService(DdcLocalConfigRepository repository,
                                                         DdcValueConverter converter) {
        return new DdcFieldBindingService(repository, converter);
    }

    @Bean
    public DdcAdminClient ddcAdminClient(DdcProperties properties) {
        return new HttpDdcAdminClient(properties);
    }

    @Bean
    public DefaultDdcConfigApplierRegistry ddcConfigApplierRegistry(
            DdcFieldBindingService fieldBindingService) {
        return new DefaultDdcConfigApplierRegistry(fieldBindingService::apply);
    }

    @Bean
    public SmartInitializingSingleton ddcConfigApplierRegistryFreezer(
            DefaultDdcConfigApplierRegistry registry) {
        return registry::freeze;
    }

    @Bean
    public DdcRefreshService ddcRefreshService(DdcLocalConfigRepository repository,
                                               DdcConfigApplierRegistry applierRegistry,
                                               DdcAdminClient adminClient,
                                               DdcLeaseSessionHolder sessionHolder) {
        return new DdcRefreshService(repository, applierRegistry, adminClient, sessionHolder);
    }

    @Bean
    public static DdcBeanPostProcessor ddcBeanPostProcessor(DdcFieldBindingService fieldBindingService) {
        return new DdcBeanPostProcessor(fieldBindingService);
    }

    @Bean(name = "ddcRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "ddcRedissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient ddcRedissonClient(DdcProperties properties) {
        DdcProperties.Redis redis = properties.getRedis();
        return Redisson.create(DdcRedisTopology.create(
                redis.getMode(),
                redis.getNodes(),
                redis.getMasterName(),
                redis.getHost(),
                redis.getPort(),
                redis.getPassword(),
                redis.getDatabase()
        ));
    }

    @Bean
    @ConditionalOnBean(name = "ddcRedissonClient")
    public DdcRedisConfigRepository ddcRedisConfigRepository(@Qualifier("ddcRedissonClient") RedissonClient redissonClient,
                                                            DdcProperties properties) {
        return new DdcRedisConfigRepository(redissonClient, properties);
    }

    @Bean
    public DdcRedisChangeListener ddcRedisChangeListener(DdcProperties properties,
                                                         DdcRefreshService refreshService) {
        return new DdcRedisChangeListener(properties, refreshService);
    }

    @Bean("ddcRedisTopic")
    @ConditionalOnBean(name = "ddcRedissonClient")
    public RTopic ddcRedisTopic(@Qualifier("ddcRedissonClient") RedissonClient redissonClient,
                                DdcProperties properties) {
        return redissonClient.getTopic(
                DdcKeys.topic(properties.getAppCode(), properties.getEnv(), properties.getNamespace())
        );
    }

    @Bean
    @ConditionalOnBean(name = "ddcRedisTopic")
    public DdcRedisChangeSubscription ddcRedisChangeSubscription(
            @Qualifier("ddcRedisTopic") RTopic topic,
            DdcRedisChangeListener listener) {
        return new DdcRedisChangeSubscription(topic, listener);
    }

    @Bean
    public DdcLeaseSessionHolder ddcLeaseSessionHolder() {
        return new DdcLeaseSessionHolder();
    }

    @Bean
    public DdcInstanceIdentity ddcInstanceIdentity(DdcProperties properties) {
        return new DdcInstanceIdentityFactory(properties).create();
    }

    @Bean
    public DdcInstanceService ddcInstanceService(DdcProperties properties,
                                                 DdcAdminClient adminClient,
                                                 DdcInstanceIdentity identity,
                                                 DdcLeaseSessionHolder sessionHolder,
                                                 ObjectProvider<DdcInstanceMetadataContributor>
                                                         metadataContributors) {
        return new DdcInstanceService(
                properties,
                adminClient,
                identity,
                sessionHolder,
                metadataContributors.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnBean(DdcRedisChangeSubscription.class)
    public DdcRuntimeCoordinator ddcRuntimeCoordinator(
            DdcProperties properties,
            DdcInstanceService instanceService,
            DdcAdminClient adminClient,
            DdcLocalConfigRepository repository,
            DdcRefreshService refreshService,
            DdcRedisChangeSubscription subscription,
            DdcLeaseSessionHolder sessionHolder) {
        return new DdcRuntimeCoordinator(
                properties,
                instanceService,
                adminClient,
                repository,
                refreshService,
                subscription,
                sessionHolder
        );
    }
}
