package ${package}.infrastructure.teaching.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ${package}.domain.teaching.service.CourseCacheService;
import ${package}.domain.teaching.vos.CourseSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import ${package}.infrastructure.teaching.validators.TeachingInfrastructureValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component("courseCacheService")
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "true")
public class RedisCourseCacheService implements CourseCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TeachingInfrastructureValidator validator;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    private final String applicationName;
    private final Duration ttl;

    public RedisCourseCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            TeachingInfrastructureValidator validator,
            TransactionCompletionExecutor transactionCompletionExecutor,
            @Value("${symbol_dollar}{spring.application.name}") String applicationName,
            @Value("${symbol_dollar}{app.integrations.redis.ttl:10m}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.transactionCompletionExecutor = transactionCompletionExecutor;
        this.applicationName = applicationName;
        this.ttl = ttl;
    }

    @Override
    public Optional<CourseSnapshot> getCourse(String courseId) {
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
    public void evictCourse(String courseId) {
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

    private String courseKey(String courseId) {
        return applicationName + ":course:" + courseId;
    }

    private String idempotencyKey(String key) {
        return applicationName + ":idempotency:course:" + key;
    }
}
