package ${package}.infrastructure.cache;

import ${package}.domain.teaching.client.SchoolClassCachePort;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.config.OrganizationIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("redisSchoolClassCache")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
public class RedisSchoolClassCache implements SchoolClassCachePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationIntegrationProperties properties;
    public RedisSchoolClassCache(RedisTemplate<String, Object> redisTemplate, OrganizationIntegrationProperties properties) {
        this.redisTemplate = redisTemplate; this.properties = properties;
    }
    @Override public Optional<SchoolClass> findById(SchoolClassId id) {
        Object value = redisTemplate.opsForValue().get(OrganizationCacheKey.schoolClass(id.value()));
        if (!(value instanceof SchoolClassCacheValue cached)) return Optional.empty();
        return Optional.of(new SchoolClass(id, cached.name(), cached.gradeId(),
            cached.gradeId().startsWith("legacy:") ? GradeCode.restoreLegacy(cached.gradeCode())
                : GradeCode.create(cached.gradeCode()), cached.gradeName(),
            SchoolClassStatus.valueOf(cached.status()), cached.userIds().stream().map(UserId::new).toList()));
    }
    @Override public void put(SchoolClass value) {
        redisTemplate.opsForValue().set(OrganizationCacheKey.schoolClass(value.id().value()),
            new SchoolClassCacheValue(value.name(), value.gradeId(), value.gradeCode().value(),
                value.gradeName(), value.status().name(), value.userIds().stream().map(UserId::value).toList()),
            properties.getSchoolClassTtl());
    }
    @Override public void evict(SchoolClassId id) { redisTemplate.delete(OrganizationCacheKey.schoolClass(id.value())); }
    private record SchoolClassCacheValue(String name, String gradeId, String gradeCode,
        String gradeName, String status, List<String> userIds) {}
}
