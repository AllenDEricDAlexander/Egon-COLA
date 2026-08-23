package ${package}.infrastructure.user.cache;

import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.vos.UserSnapshot;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class InMemoryUserCacheService implements UserCacheService {
    private final ConcurrentMap<String, UserSnapshot> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> claims = new ConcurrentHashMap<>();
    private final TransactionCompletionExecutor transactionCompletionExecutor;

    @Override
    public Optional<UserSnapshot> getUser(long userId) {
        return Optional.ofNullable(users.get(Long.toString(userId)));
    }

    @Override
    public void putUser(UserSnapshot user) {
        users.put(Long.toString(user.id()), user);
    }

    @Override
    public void evictUser(long userId) {
        transactionCompletionExecutor.executeAfterCommit(() -> users.remove(Long.toString(userId)));
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
