package top.egon.cola.archetype.source.light.domain.user.client;

import top.egon.cola.archetype.source.light.domain.user.vos.UserSnapshot;

import java.time.Duration;
import java.util.Optional;

/** Outbound cache port owned by the user domain. */
public interface UserCachePort {
    Optional<UserSnapshot> getUser(Long userId);

    void putUser(UserSnapshot user);

    void evictUser(Long userId);

    boolean claimIdempotency(String key, Duration ttl);
}
