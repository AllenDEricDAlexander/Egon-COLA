package top.egon.cola.archetype.source.light.infrastructure.teaching.cache;

import top.egon.cola.archetype.source.light.domain.teaching.client.CourseCachePort;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSnapshot;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Slf4j
public class InMemoryCourseCacheService implements CourseCachePort {
    private final ConcurrentMap<Long, CourseSnapshot> courses = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> claims = new ConcurrentHashMap<>();
    private final TransactionCompletionExecutor transactionCompletionExecutor;

    @Override
    public Optional<CourseSnapshot> getCourse(Long courseId) {
        return Optional.ofNullable(courses.get(courseId));
    }

    @Override
    public void putCourse(CourseSnapshot course) {
        courses.put(course.id(), course);
    }

    @Override
    public void evictCourse(Long courseId) {
        transactionCompletionExecutor.executeAfterCommit(() -> courses.remove(courseId));
    }

    @Override
    public boolean claimIdempotency(String key, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        AtomicBoolean claimed = new AtomicBoolean();
        claims.compute(key, (ignored, current) -> {
            if (current == null || current.isBefore(Instant.now())) {
                claimed.set(true);
                return expiresAt;
            }
            return current;
        });
        if (claimed.get()) {
            transactionCompletionExecutor.executeAfterRollback(() -> claims.remove(key, expiresAt));
        }
        return claimed.get();
    }
}
