package ${package}.domain.user.service;

import ${package}.domain.user.vos.UserSnapshot;

import java.time.Duration;
import java.util.Optional;

public interface UserCacheService {
    Optional<UserSnapshot> getUser(long userId);

    void putUser(UserSnapshot user);

    void evictUser(long userId);

    boolean claimIdempotency(String key, Duration ttl);
}
