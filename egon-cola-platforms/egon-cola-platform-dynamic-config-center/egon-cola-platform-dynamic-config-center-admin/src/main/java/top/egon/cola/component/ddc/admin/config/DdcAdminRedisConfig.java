package top.egon.cola.component.ddc.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.admin.security.openapi.DdcNonceStore;
import top.egon.cola.component.ddc.admin.security.openapi.InMemoryDdcNonceStore;
import top.egon.cola.component.ddc.admin.security.openapi.RedisDdcNonceStore;
import top.egon.cola.component.ddc.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseExpiryScanner;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.redis.DdcRedisClientFactory;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DdcAdminProperties.class)
public class DdcAdminRedisConfig {

    @Bean("ddcAdminRedissonClient")
    @ConditionalOnMissingBean(name = "ddcAdminRedissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc.admin.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient ddcAdminRedissonClient(DdcAdminProperties properties) {
        DdcAdminProperties.Redis redis = properties.getRedis();
        return DdcRedisClientFactory.create(
                redis.getMode(),
                redis.getNodes(),
                redis.getMasterName(),
                redis.getHost(),
                redis.getPort(),
                redis.getPassword(),
                redis.getDatabase()
        );
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcRedisRepository ddcRedisRepository(@Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient) {
        return new DdcRedisRepository(redissonClient);
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean(DdcNonceStore.class)
    public DdcNonceStore redisDdcNonceStore(
            @Qualifier("ddcAdminRedissonClient")
            RedissonClient redissonClient) {
        return new RedisDdcNonceStore(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(DdcNonceStore.class)
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.admin.security",
            name = "local-dev",
            havingValue = "true"
    )
    public DdcNonceStore inMemoryDdcNonceStore(
            DdcAdminProperties properties) {
        return new InMemoryDdcNonceStore(
                properties.getOpenapi().getNonceCacheMaxSize()
        );
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcConfigLeaseRedisRepository ddcConfigLeaseRedisRepository(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            ObjectMapper objectMapper) {
        return new DdcConfigLeaseRedisRepository(redissonClient, objectMapper);
    }

    @Bean
    @ConditionalOnBean(DdcConfigLeaseRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcConfigLeaseService ddcConfigLeaseService(DdcConfigLeaseRedisRepository repository,
                                                       DdcAdminProperties properties) {
        return new DdcConfigLeaseService(repository, new DdcLeaseValidator(properties));
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcServiceRegistryRedisRepository ddcServiceRegistryRedisRepository(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            ObjectMapper objectMapper) {
        return new DdcServiceRegistryRedisRepository(redissonClient, objectMapper);
    }

    @Bean
    @ConditionalOnBean(DdcServiceRegistryRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcServiceRegistryService ddcServiceRegistryService(
            DdcServiceRegistryRedisRepository repository,
            DdcAdminProperties properties,
            DdcScopeGate scopeGate) {
        return new DdcServiceRegistryService(
                repository,
                new DdcLeaseValidator(properties),
                scopeGate
        );
    }

    @Bean
    @ConditionalOnBean(DdcConfigLeaseRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcLeaseExpiryScanner ddcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                                                       DdcConfigLeaseRedisRepository leaseRepository,
                                                       DdcServiceRegistryRedisRepository registryRepository) {
        return new DdcLeaseExpiryScanner(
                instanceRepository,
                leaseRepository,
                registryRepository
        );
    }
}
