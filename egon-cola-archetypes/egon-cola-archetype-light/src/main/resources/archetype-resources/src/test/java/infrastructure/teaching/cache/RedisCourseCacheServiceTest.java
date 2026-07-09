package ${package}.infrastructure.teaching.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import ${package}.infrastructure.teaching.validators.TeachingInfrastructureValidator;
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
                "course-1", new CourseCode("COURSE-001"), "Mathematics", CourseStatus.ACTIVE);
        String json = objectMapper.writeValueAsString(snapshot);
        when(values.get("sample:course:course-1")).thenReturn(json);

        cache.putCourse(snapshot);

        assertEquals(snapshot, cache.getCourse("course-1").orElseThrow());
        verify(values).set("sample:course:course-1", json, Duration.ofMinutes(10));
    }
}
