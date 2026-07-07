package top.egon.cola.component.ddc.config;

import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.client.HttpDdcAdminClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.listener.DdcRedisChangeListener;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.processor.DdcBeanPostProcessor;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;
import top.egon.cola.component.ddc.repository.DdcRedisConfigRepository;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;
import top.egon.cola.component.ddc.service.DdcInstanceService;
import top.egon.cola.component.ddc.service.DdcRefreshService;

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
    public DdcRefreshService ddcRefreshService(DdcLocalConfigRepository repository,
                                               DdcFieldBindingService fieldBindingService,
                                               DdcAdminClient adminClient) {
        return new DdcRefreshService(repository, fieldBindingService::apply, adminClient);
    }

    @Bean
    public static DdcBeanPostProcessor ddcBeanPostProcessor(DdcFieldBindingService fieldBindingService) {
        return new DdcBeanPostProcessor(fieldBindingService);
    }

    @Bean("ddcRedissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient ddcRedissonClient(DdcProperties properties) {
        Config config = new Config();
        DdcProperties.Redis redis = properties.getRedis();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
                .setDatabase(redis.getDatabase());
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            config.useSingleServer().setPassword(redis.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
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
    @ConditionalOnBean(RedissonClient.class)
    public RTopic ddcRedisTopic(@Qualifier("ddcRedissonClient") RedissonClient redissonClient,
                                DdcProperties properties,
                                DdcRedisChangeListener listener) {
        RTopic topic = redissonClient.getTopic(DdcKeys.topic(properties.getAppCode(), properties.getEnv(), properties.getNamespace()));
        topic.addListener(DdcPublishMessage.class, listener);
        return topic;
    }

    @Bean
    public DdcInstanceService ddcInstanceService(DdcProperties properties,
                                                 DdcAdminClient adminClient) {
        return new DdcInstanceService(properties, adminClient);
    }
}
