package top.egon.cola.archetype.source.light.infrastructure.teaching.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSnapshot;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
import top.egon.cola.archetype.source.light.infrastructure.teaching.validators.TeachingInfrastructureValidator;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCourseCacheServiceTest {
    @Test
    void round_trips_namespaced_course_json() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisCourseCacheService cache = new RedisCourseCacheService(
                redis, objectMapper, new TeachingInfrastructureValidator(),
                new TransactionCompletionExecutor(), "sample", Duration.ofMinutes(10));
        CourseSnapshot snapshot = new CourseSnapshot(
                1002L, new CourseCode("COURSE-001"), "Mathematics", CourseStatus.ACTIVE);
        String json = objectMapper.writeValueAsString(snapshot);
        when(values.get("sample:course:1002")).thenReturn(json);

        cache.putCourse(snapshot);

        assertEquals(snapshot, cache.getCourse(1002L).orElseThrow());
        verify(values).set("sample:course:1002", json, Duration.ofMinutes(10));
    }
}
