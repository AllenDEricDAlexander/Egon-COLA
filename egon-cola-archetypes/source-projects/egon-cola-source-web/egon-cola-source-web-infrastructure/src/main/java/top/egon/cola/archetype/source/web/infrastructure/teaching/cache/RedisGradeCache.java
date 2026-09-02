package top.egon.cola.archetype.source.web.infrastructure.teaching.cache;

import top.egon.cola.archetype.source.web.domain.teaching.client.GradeCachePort;
import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.infrastructure.cache.OrganizationCacheKey;
import top.egon.cola.archetype.source.web.infrastructure.config.OrganizationIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("redisGradeCache")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisGradeCache implements GradeCachePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationIntegrationProperties properties;
    @Override public Optional<Grade> findById(Long id) {
        Object value = redisTemplate.opsForValue().get(OrganizationCacheKey.grade(id));
        if (!(value instanceof GradeCacheValue cached)) return Optional.empty();
        return Optional.of(new Grade(cached.id(), GradeCode.create(cached.code()),
            cached.name(), GradeStatus.valueOf(cached.status())));
    }
    @Override public void put(Grade grade) {
        redisTemplate.opsForValue().set(OrganizationCacheKey.grade(grade.id()),
            new GradeCacheValue(grade.id(), grade.code().value(), grade.name(), grade.status().name()),
            properties.getGradeTtl());
    }
    @Override public void evict(Long id) { redisTemplate.delete(OrganizationCacheKey.grade(id)); }
    private record GradeCacheValue(Long id, String code, String name, String status) {}
}
