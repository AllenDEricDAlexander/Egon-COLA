package top.egon.cola.archetype.source.web.infrastructure.teaching.cache;

import top.egon.cola.archetype.source.web.domain.teaching.client.SchoolClassCachePort;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.archetype.source.web.infrastructure.cache.OrganizationCacheKey;
import top.egon.cola.archetype.source.web.infrastructure.config.OrganizationIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("redisSchoolClassCache")
@ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisSchoolClassCache implements SchoolClassCachePort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationIntegrationProperties properties;
    @Override public Optional<SchoolClass> findById(Long gradeId, SchoolClassId id) {
        Object value = redisTemplate.opsForValue().get(
                OrganizationCacheKey.schoolClass(gradeId, id.value()));
        if (!(value instanceof SchoolClassCacheValue cached)) return Optional.empty();
        return Optional.of(new SchoolClass(id, cached.name(), cached.gradeId(),
            GradeCode.create(cached.gradeCode()), cached.gradeName(),
            SchoolClassStatus.valueOf(cached.status()), cached.userIds().stream().map(UserId::new).toList()));
    }
    @Override public void put(SchoolClass value) {
        redisTemplate.opsForValue().set(
            OrganizationCacheKey.schoolClass(value.gradeId(), value.id().value()),
            new SchoolClassCacheValue(value.name(), value.gradeId(), value.gradeCode().value(),
                value.gradeName(), value.status().name(), value.userIds().stream().map(UserId::value).toList()),
            properties.getSchoolClassTtl());
    }
    @Override public void evict(Long gradeId, SchoolClassId id) {
        redisTemplate.delete(OrganizationCacheKey.schoolClass(gradeId, id.value()));
    }
    private record SchoolClassCacheValue(String name, Long gradeId, String gradeCode,
        String gradeName, String status, List<Long> userIds) {}
}
