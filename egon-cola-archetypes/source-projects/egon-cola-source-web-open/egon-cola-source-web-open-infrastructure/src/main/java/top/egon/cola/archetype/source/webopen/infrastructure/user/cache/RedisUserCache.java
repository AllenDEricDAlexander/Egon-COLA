package top.egon.cola.archetype.source.webopen.infrastructure.user.cache;

import top.egon.cola.archetype.source.webopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.webopen.infrastructure.cache.OrganizationCacheKey;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("redisUserCache")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisUserCache implements UserCachePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationIntegrationProperties properties;

    @Override public Optional<User> findById(UserId id) {
        Object value = redisTemplate.opsForValue().get(OrganizationCacheKey.user(id.value()));
        if (!(value instanceof UserCacheValue cached)) return Optional.empty();
        return Optional.of(User.restore(new UserId(cached.id()), cached.name(), cached.email(),
            UserStatus.valueOf(cached.status()), cached.roleCodes().stream().map(RoleCode::new).toList()));
    }

    @Override public void put(User user) {
        redisTemplate.opsForValue().set(OrganizationCacheKey.user(user.id().value()),
            new UserCacheValue(user.id().value(), user.name(), user.email(), user.status().name(),
                user.roleCodes().stream().map(RoleCode::value).toList()), properties.getUserTtl());
    }

    @Override public void evict(UserId id) { redisTemplate.delete(OrganizationCacheKey.user(id.value())); }

    private record UserCacheValue(Long id, String name, String email, String status, List<String> roleCodes) {}
}
