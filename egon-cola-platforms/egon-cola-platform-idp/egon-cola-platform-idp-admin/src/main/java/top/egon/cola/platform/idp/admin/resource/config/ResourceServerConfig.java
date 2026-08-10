package top.egon.cola.platform.idp.admin.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;

/**
 * Resource Server 管理域的运行态投影装配。
 *
 * <p>Runtime projection wiring for Resource Server administration.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ResourceServerConfig {

    /**
     * 创建 Redis 运行态投影服务。
     *
     * <p>Creates the Redis runtime projection service.</p>
     *
     * @param redisson 身份运行态 Redis 客户端；identity-runtime Redis client
     * @param objectMapper JSON 编解码器；JSON codec
     * @return Resource Server 投影服务；Resource Server projection service
     */
    @Bean
    ResourceServerProjectionService resourceServerProjectionService(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper
    ) {
        return new ResourceServerProjectionService(redisson, objectMapper);
    }
}
