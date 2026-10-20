package top.egon.cola.component.yaoguang.admin.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 中文说明：提供独立知识宿主的调度、JSON 和缓存桥接，不装配网关发布或供应商凭据组件。
 * English summary: Supplies scheduling, JSON and the existing identity cache bridge for the knowledge host.
 */
@Slf4j
@EnableScheduling
@Configuration(value = "yaoguangAdminConfiguration", proxyBeanMethods = false)
public class YaoguangAdminConfiguration {

    @Bean(name = "redissonClient", destroyMethod = "")
    @ConditionalOnMissingBean(name = "redissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    org.redisson.api.RedissonClient applicationCacheRedissonClient(
            @Qualifier("rbac3RuntimeRedissonClient") org.redisson.api.RedissonClient client) {
        return client;
    }

    @Bean(name = "jacksonObjectMapper")
    @Primary
    ObjectMapper jacksonObjectMapper() {
        return JsonMapper.builder().findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    }
}
