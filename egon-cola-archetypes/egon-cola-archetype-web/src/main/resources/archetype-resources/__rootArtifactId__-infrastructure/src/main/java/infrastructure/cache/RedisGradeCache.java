package ${package}.infrastructure.cache;

import ${package}.domain.teaching.client.GradeCachePort;
import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.infrastructure.config.OrganizationIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("redisGradeCache")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
public class RedisGradeCache implements GradeCachePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationIntegrationProperties properties;
    public RedisGradeCache(RedisTemplate<String, Object> redisTemplate, OrganizationIntegrationProperties properties) {
        this.redisTemplate = redisTemplate; this.properties = properties;
    }
    @Override public Optional<Grade> findById(String id) {
        Object value = redisTemplate.opsForValue().get(OrganizationCacheKey.grade(id));
        if (!(value instanceof GradeCacheValue cached)) return Optional.empty();
        GradeCode code = cached.id().startsWith("legacy:")
            ? GradeCode.restoreLegacy(cached.code()) : GradeCode.create(cached.code());
        return Optional.of(new Grade(cached.id(), code, cached.name(), GradeStatus.valueOf(cached.status())));
    }
    @Override public void put(Grade grade) {
        redisTemplate.opsForValue().set(OrganizationCacheKey.grade(grade.id()),
            new GradeCacheValue(grade.id(), grade.code().value(), grade.name(), grade.status().name()),
            properties.getGradeTtl());
    }
    @Override public void evict(String id) { redisTemplate.delete(OrganizationCacheKey.grade(id)); }
    private record GradeCacheValue(String id, String code, String name, String status) {}
}
