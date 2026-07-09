package ${package}.infrastructure.teaching.cache;

import ${package}.domain.teaching.service.CourseCacheService;
import ${package}.domain.teaching.vos.CourseSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemoryCourseCacheService implements CourseCacheService {
    private final ConcurrentMap<String, CourseSnapshot> courses = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> claims = new ConcurrentHashMap<>();
    private final TransactionCompletionExecutor transactionCompletionExecutor;

    public InMemoryCourseCacheService(TransactionCompletionExecutor transactionCompletionExecutor) {
        this.transactionCompletionExecutor = transactionCompletionExecutor;
    }

    @Override
    public Optional<CourseSnapshot> getCourse(String courseId) {
        return Optional.ofNullable(courses.get(courseId));
    }

    @Override
    public void putCourse(CourseSnapshot course) {
        courses.put(course.id(), course);
    }

    @Override
    public void evictCourse(String courseId) {
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
