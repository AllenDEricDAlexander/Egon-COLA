package top.egon.cola.component.ddc.config;

import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.client.HttpDdcAdminClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.listener.DdcRedisChangeListener;
import top.egon.cola.component.ddc.listener.DdcRedisChangeSubscription;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.processor.DdcBeanPostProcessor;
import top.egon.cola.component.ddc.refresh.DdcConfigurationPropertiesRebinder;
import top.egon.cola.component.ddc.refresh.DdcYamlConfigApplier;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcAckDelivery;
import top.egon.cola.component.ddc.service.DdcAckDeliveryProperties;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;
import top.egon.cola.component.ddc.service.DdcInstanceIdentityFactory;
import top.egon.cola.component.ddc.service.DdcInstanceIdProvider;
import top.egon.cola.component.ddc.service.DdcInstanceService;
import top.egon.cola.component.ddc.service.DdcInstanceMetadataContributor;
import top.egon.cola.component.ddc.service.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.service.DdcRuntimeCoordinator;

import java.util.List;

/**
 * 在 DDC 启用时装配配置注入、远程租约、Redis 订阅和刷新运行时。 Configures value injection, remote leases, Redis subscription, and refresh runtime when DDC is enabled.
 */
@AutoConfiguration
@EnableScheduling
@ComponentScan(basePackageClasses = DdcLocalConfigRepository.class)
@EnableConfigurationProperties({
        DdcProperties.class,
        DdcAckDeliveryProperties.class
})
@ConditionalOnProperty(prefix = "egon.cola.component.ddc", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DdcAutoConfig {

    /** 用于报告显式离线模式的日志记录器。 Logger used to report explicit offline mode. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DdcAutoConfig.class);

    /**
     * 创建 DDC 文本值转换器。 Creates the DDC text-value converter.
     * @return DDC 文本值转换器。 DDC text-value converter
     */
    @Bean
    public DdcValueConverter ddcValueConverter() {
        return new DdcValueConverter();
    }

    /**
     * 创建负责解析并注入 {@code @DdcValue} 字段的绑定服务。 Creates the binding service that resolves and injects {@code @DdcValue} fields.
     * @param repository 本地配置仓库。 local configuration repository
     * @param converter 值转换器。 value converter
     * @param environment Spring 环境。 Spring environment
     * @return 字段绑定服务。 field-binding service
     */
    @Bean
    public DdcFieldBindingService ddcFieldBindingService(DdcLocalConfigRepository repository,
                                                         DdcValueConverter converter,
                                                         ConfigurableEnvironment environment) {
        return new DdcFieldBindingService(
                repository,
                converter,
                environment
        );
    }

    /**
     * 在应用未提供替代实现时创建 HTTP 管理端客户端。 Creates the HTTP management client when the application supplies no replacement.
     * @param properties DDC 属性。 DDC properties
     * @return 管理端客户端。 management client
     */
    @Bean
    @ConditionalOnMissingBean(DdcAdminClient.class)
    public DdcAdminClient ddcAdminClient(DdcProperties properties) {
        return new HttpDdcAdminClient(properties);
    }

    /**
     * 创建默认配置应用器注册表，并注册字段绑定应用器。 Creates the default configuration-applier registry with the field-binding applier registered.
     * @param fieldBindingService 字段绑定服务。 field-binding service
     * @return 默认应用器注册表。 default applier registry
     */
    @Bean
    public DefaultDdcConfigApplierRegistry ddcConfigApplierRegistry(
            DdcFieldBindingService fieldBindingService) {
        return new DefaultDdcConfigApplierRegistry(fieldBindingService::apply);
    }

    /**
     * 创建在所有单例完成装配后冻结应用器注册表的回调。 Creates a callback that freezes the applier registry after singleton initialization.
     * @param registry 待冻结的注册表。 registry to freeze
     * @return 单例初始化完成回调。 singleton-initialization callback
     */
    @Bean
    public SmartInitializingSingleton ddcConfigApplierRegistryFreezer(
            DefaultDdcConfigApplierRegistry registry) {
        return registry::freeze;
    }

    /**
     * 创建配置属性 Bean 的重新绑定器。 Creates the rebinder for configuration-properties beans.
     * @param applicationContext 应用上下文。 application context
     * @param bindingPostProcessor 配置属性绑定后处理器。 configuration-properties binding post-processor
     * @param environment Spring 环境。 Spring environment
     * @return 配置属性重新绑定器。 configuration-properties rebinder
     */
    @Bean
    public DdcConfigurationPropertiesRebinder
    ddcConfigurationPropertiesRebinder(
            ApplicationContext applicationContext,
            ConfigurationPropertiesBindingPostProcessor bindingPostProcessor,
            ConfigurableEnvironment environment) {
        return new DdcConfigurationPropertiesRebinder(
                applicationContext,
                bindingPostProcessor,
                environment
        );
    }

    /**
     * 在远程生命周期启用时创建具备大小校验和事件发布的 YAML 应用器。 Creates the YAML applier with size validation and event publication when remote lifecycle is enabled.
     * @param environment Spring 环境。 Spring environment
     * @param applierRegistry 配置应用器注册表。 configuration-applier registry
     * @param fieldBindingService 字段绑定服务。 field-binding service
     * @param rebinder 配置属性重新绑定器。 configuration-properties rebinder
     * @param eventPublisher 应用事件发布器。 application event publisher
     * @param properties DDC 属性。 DDC properties
     * @return YAML 配置应用器。 YAML configuration applier
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public DdcYamlConfigApplier ddcYamlConfigApplier(
            ConfigurableEnvironment environment,
            DdcConfigApplierRegistry applierRegistry,
            DdcFieldBindingService fieldBindingService,
            DdcConfigurationPropertiesRebinder rebinder,
            ApplicationEventPublisher eventPublisher,
            DdcProperties properties) {
        return new DdcYamlConfigApplier(
                environment,
                applierRegistry,
                fieldBindingService,
                rebinder,
                eventPublisher,
                properties.getMaxYamlBytes()
        );
    }

    /**
     * 在 Redis 被显式禁用时创建启动告警回调，说明远程生命周期不会运行。 Creates a startup warning callback explaining that remote lifecycle will not run when Redis is explicitly disabled.
     * @return 输出离线模式告警的回调。 callback that logs the offline-mode warning
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.redis",
            name = "enabled",
            havingValue = "false"
    )
    public SmartInitializingSingleton ddcOfflineModeWarning() {
        return () -> LOGGER.warn(
                "DDC remote lifecycle is disabled because "
                        + "egon.cola.component.ddc.redis.enabled=false; "
                        + "no registration, pull, subscription, heartbeat, or ACK will run"
        );
    }

    /**
     * 在缺少替代实现时创建带重试能力的发布确认投递器。 Creates the retry-capable publication acknowledgement delivery when no replacement exists.
     * @param adminClient 管理端客户端。 management client
     * @param properties 确认投递属性。 acknowledgement-delivery properties
     * @return 发布确认投递器。 publication acknowledgement delivery
     */
    @Bean
    @ConditionalOnMissingBean
    public DdcAckDelivery ddcAckDelivery(
            DdcAdminClient adminClient,
            DdcAckDeliveryProperties properties) {
        return new DdcAckDelivery(adminClient, properties);
    }

    /**
     * 在远程生命周期启用时创建校验版本、应用 YAML 并投递确认的刷新服务。 Creates the refresh service that validates versions, applies YAML, and delivers acknowledgements when remote lifecycle is enabled.
     * @param repository 本地配置仓库。 local configuration repository
     * @param yamlConfigApplier YAML 配置应用器。 YAML configuration applier
     * @param ackDelivery 确认投递器。 acknowledgement delivery
     * @param sessionHolder 当前租约会话持有器。 current lease-session holder
     * @return 配置刷新服务。 configuration refresh service
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public DdcRefreshService ddcRefreshService(DdcLocalConfigRepository repository,
                                               DdcYamlConfigApplier yamlConfigApplier,
                                               DdcAckDelivery ackDelivery,
                                               DdcLeaseSessionHolder sessionHolder) {
        return new DdcRefreshService(
                repository,
                yamlConfigApplier,
                ackDelivery,
                sessionHolder
        );
    }

    /**
     * 创建静态 Bean 后处理器，在 Bean 初始化期间发现并绑定 DDC 字段。 Creates the static bean post-processor that discovers and binds DDC fields during bean initialization.
     * @param fieldBindingService 字段绑定服务。 field-binding service
     * @return DDC Bean 后处理器。 DDC bean post-processor
     */
    @Bean
    public static DdcBeanPostProcessor ddcBeanPostProcessor(DdcFieldBindingService fieldBindingService) {
        return new DdcBeanPostProcessor(fieldBindingService);
    }

    /**
     * 创建 DDC 配置生命周期专用且随容器关闭的 Redisson 客户端。 Creates the dedicated Redisson client for DDC configuration lifecycle and shuts it down with the container.
     * @param properties DDC 属性。 DDC properties
     * @return DDC Redisson 客户端。 DDC Redisson client
     */
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

    /**
     * 创建负责校验发布消息并触发刷新的 Redis 监听器。 Creates the Redis listener that validates publication messages and triggers refresh.
     * @param properties DDC 作用域属性。 DDC scope properties
     * @param refreshService 配置刷新服务。 configuration refresh service
     * @return Redis 配置变更监听器。 Redis configuration-change listener
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public DdcRedisChangeListener ddcRedisChangeListener(DdcProperties properties,
                                                         DdcRefreshService refreshService) {
        return new DdcRedisChangeListener(properties, refreshService);
    }

    /**
     * 创建当前业务、环境和应用 v3 配置变更 Topic。 Creates the v3 configuration-change topic for the current business, environment, and application.
     * @param redissonClient DDC Redisson 客户端。 DDC Redisson client
     * @param properties DDC 作用域属性。 DDC scope properties
     * @return 配置变更 Topic。 configuration-change topic
     */
    @Bean("ddcRedisTopic")
    @ConditionalOnBean(name = "ddcRedissonClient")
    public RTopic ddcRedisTopic(@Qualifier("ddcRedissonClient") RedissonClient redissonClient,
                                DdcProperties properties) {
        return redissonClient.getTopic(
                DdcKeys.v3Topic(
                        properties.getBizCode(),
                        properties.getEnv(),
                        properties.getAppCode()
                )
        );
    }

    /**
     * 创建随 Spring 容器关闭而解除监听的 Redis 订阅句柄。 Creates the Redis subscription handle whose listener is removed when the Spring container closes.
     * @param topic 配置变更 Topic。 configuration-change topic
     * @param listener 配置变更监听器。 configuration-change listener
     * @return Redis 订阅句柄。 Redis subscription handle
     */
    @Bean
    @ConditionalOnBean(name = "ddcRedisTopic")
    public DdcRedisChangeSubscription ddcRedisChangeSubscription(
            @Qualifier("ddcRedisTopic") RTopic topic,
            DdcRedisChangeListener listener) {
        return new DdcRedisChangeSubscription(List.of(topic), listener);
    }

    /**
     * 创建保存当前实例租约会话的线程安全持有器。 Creates the thread-safe holder for the current instance lease session.
     * @return 租约会话持有器。 lease-session holder
     */
    @Bean
    public DdcLeaseSessionHolder ddcLeaseSessionHolder() {
        return new DdcLeaseSessionHolder();
    }

    /**
     * 从属性和可选自定义提供器创建稳定实例身份。 Creates a stable instance identity from properties and an optional custom provider.
     * @param properties DDC 属性。 DDC properties
     * @param instanceIdProvider 可选实例标识提供器。 optional instance-identifier provider
     * @return 当前 DDC 实例身份。 current DDC instance identity
     */
    @Bean
    public DdcInstanceIdentity ddcInstanceIdentity(
            DdcProperties properties,
            ObjectProvider<DdcInstanceIdProvider> instanceIdProvider) {
        return new DdcInstanceIdentityFactory(
                properties,
                instanceIdProvider.getIfAvailable()
        ).create();
    }

    /**
     * 创建负责注册、心跳和下线的实例租约服务。 Creates the instance-lease service responsible for registration, heartbeat, and offline operations.
     * @param properties DDC 属性。 DDC properties
     * @param adminClient 管理端客户端。 management client
     * @param identity 当前实例身份。 current instance identity
     * @param sessionHolder 租约会话持有器。 lease-session holder
     * @param metadataContributors 有序实例元数据贡献器。 ordered instance-metadata contributors
     * @return DDC 实例服务。 DDC instance service
     */
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

    /**
     * 在 Redis 订阅存在时创建编排注册、初始拉取、对账和关闭的运行时协调器。 Creates the runtime coordinator orchestrating registration, initial pull, reconciliation, and shutdown when a Redis subscription exists.
     * @param properties DDC 属性。 DDC properties
     * @param instanceService 实例租约服务。 instance-lease service
     * @param adminClient 管理端客户端。 management client
     * @param refreshService 配置刷新服务。 configuration refresh service
     * @param subscription Redis 订阅句柄。 Redis subscription handle
     * @param sessionHolder 租约会话持有器。 lease-session holder
     * @return DDC 运行时协调器。 DDC runtime coordinator
     */
    @Bean
    @ConditionalOnBean(DdcRedisChangeSubscription.class)
    public DdcRuntimeCoordinator ddcRuntimeCoordinator(
            DdcProperties properties,
            DdcInstanceService instanceService,
            DdcAdminClient adminClient,
            DdcRefreshService refreshService,
            DdcRedisChangeSubscription subscription,
            DdcLeaseSessionHolder sessionHolder) {
        return new DdcRuntimeCoordinator(
                properties,
                instanceService,
                adminClient,
                refreshService,
                subscription,
                sessionHolder
        );
    }
}
