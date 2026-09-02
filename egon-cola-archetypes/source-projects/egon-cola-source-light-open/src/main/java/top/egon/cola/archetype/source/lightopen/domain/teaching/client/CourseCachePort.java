package top.egon.cola.archetype.source.lightopen.domain.teaching.client;

import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSnapshot;

import java.time.Duration;
import java.util.Optional;

/** Outbound cache port owned by the teaching domain. */
public interface CourseCachePort {
    Optional<CourseSnapshot> getCourse(Long courseId);

    void putCourse(CourseSnapshot course);

    void evictCourse(Long courseId);

    boolean claimIdempotency(String key, Duration ttl);
}
