package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.archetype.source.lightopen.domain.teaching.client.CourseCachePort;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSnapshot;
import top.egon.cola.archetype.source.lightopen.infrastructure.config.TransactionCompletionExecutor;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.validators.TeachingInfrastructureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component("courseCachePort")
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RedisCourseCacheService implements CourseCachePort {
    @Qualifier("stringRedisTemplate")
    private final StringRedisTemplate redisTemplate;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("teachingInfrastructureValidator")
    private final TeachingInfrastructureValidator validator;
    @Qualifier("transactionCompletionExecutor")
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${spring.application.name}")
    private final String applicationName;
    @Value("${app.integrations.redis.ttl:10m}")
    private final Duration ttl;

    @Override
    public Optional<CourseSnapshot> getCourse(Long courseId) {
        String payload = redisTemplate.opsForValue().get(courseKey(courseId));
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, CourseSnapshot.class));
        } catch (JsonProcessingException exception) {
            throw validator.invalidCachePayload(payload, exception);
        }
    }

    @Override
    public void putCourse(CourseSnapshot course) {
        try {
            redisTemplate.opsForValue().set(
                    courseKey(course.id()), objectMapper.writeValueAsString(course), ttl);
        } catch (JsonProcessingException exception) {
            throw validator.invalidCachePayload(course.id(), exception);
        }
    }

    @Override
    public void evictCourse(Long courseId) {
        transactionCompletionExecutor.executeAfterCommit(() -> redisTemplate.delete(courseKey(courseId)));
    }

    @Override
    public boolean claimIdempotency(String key, Duration claimTtl) {
        String redisKey = idempotencyKey(key);
        boolean claimed = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(redisKey, "1", claimTtl));
        if (claimed) {
            transactionCompletionExecutor.executeAfterRollback(() -> redisTemplate.delete(redisKey));
        }
        return claimed;
    }

    private String courseKey(Long courseId) {
        return applicationName + ":course:" + courseId;
    }

    private String idempotencyKey(String key) {
        return applicationName + ":idempotency:course:" + key;
    }
}
