package top.egon.cola.component.ddc.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.service.DdcConfigLeaseService;
import top.egon.cola.component.ddc.admin.service.DdcLeaseValidator;

@Configuration
@EnableConfigurationProperties(DdcAdminProperties.class)
public class DdcAdminRedisConfig {

    @Bean("ddcAdminRedissonClient")
    @ConditionalOnMissingBean(name = "ddcAdminRedissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc.admin.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient ddcAdminRedissonClient(DdcAdminProperties properties) {
        DdcAdminProperties.Redis redis = properties.getRedis();
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
                .setDatabase(redis.getDatabase());
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            config.useSingleServer().setPassword(redis.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcRedisRepository ddcRedisRepository(@Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient) {
        return new DdcRedisRepository(redissonClient);
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
}
