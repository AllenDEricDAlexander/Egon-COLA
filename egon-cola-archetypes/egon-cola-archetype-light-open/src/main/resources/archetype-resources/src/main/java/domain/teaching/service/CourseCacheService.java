package ${package}.domain.teaching.service;

import ${package}.domain.teaching.vos.CourseSnapshot;

import java.time.Duration;
import java.util.Optional;

public interface CourseCacheService {
    Optional<CourseSnapshot> getCourse(long courseId);

    void putCourse(CourseSnapshot course);

    void evictCourse(long courseId);

    boolean claimIdempotency(String key, Duration ttl);
}
