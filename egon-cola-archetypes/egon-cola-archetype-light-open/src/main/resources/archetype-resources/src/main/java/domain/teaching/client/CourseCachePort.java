package ${package}.domain.teaching.client;

import ${package}.domain.teaching.vos.CourseSnapshot;

import java.time.Duration;
import java.util.Optional;

/** Outbound cache port owned by the teaching domain. */
public interface CourseCachePort {
    Optional<CourseSnapshot> getCourse(Long courseId);

    void putCourse(CourseSnapshot course);

    void evictCourse(Long courseId);

    boolean claimIdempotency(String key, Duration ttl);
}
