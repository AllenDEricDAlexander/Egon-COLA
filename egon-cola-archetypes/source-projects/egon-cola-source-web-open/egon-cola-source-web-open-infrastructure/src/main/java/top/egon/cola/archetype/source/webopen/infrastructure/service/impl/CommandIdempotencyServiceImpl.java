package top.egon.cola.archetype.source.webopen.infrastructure.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.infrastructure.cache.OrganizationCacheKey;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationIntegrationProperties;

/**
 * Redis claim/release behind the idempotency Domain Service. The key scope, value and TTL stay
 * exactly as the previous adapter published them, so a claim taken before this class existed is
 * still honoured afterwards.
 */
@Validated
@Service("commandIdempotencyService")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class CommandIdempotencyServiceImpl implements CommandIdempotencyService {
    @Qualifier("organizationRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("organizationIntegrationProperties")
    private final OrganizationIntegrationProperties properties;

    @Override
    public boolean claim(String operation, String requestId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                OrganizationCacheKey.command(operation, requestId), "1", properties.getCommandIdempotencyTtl()));
    }

    @Override
    public void release(String operation, String requestId) {
        redisTemplate.delete(OrganizationCacheKey.command(operation, requestId));
    }
}
