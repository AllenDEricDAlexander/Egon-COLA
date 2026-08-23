package ${package}.infrastructure.cache;

import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.infrastructure.config.OrganizationIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("redisCommandIdempotencyAdapter")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisCommandIdempotencyAdapter implements CommandIdempotencyPort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationIntegrationProperties properties;
    @Override public boolean claim(String operation, String requestId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
            OrganizationCacheKey.command(operation, requestId), "1", properties.getCommandIdempotencyTtl()));
    }
    @Override public void release(String operation, String requestId) {
        redisTemplate.delete(OrganizationCacheKey.command(operation, requestId));
    }
}
