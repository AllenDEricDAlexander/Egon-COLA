package top.egon.cola.archetype.source.web.infrastructure.user.cache;

import top.egon.cola.archetype.source.web.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserCache implements UserCachePort {
    private final ConcurrentHashMap<UserId, User> values = new ConcurrentHashMap<>();
    @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(values.get(id)); }
    @Override public void put(User user) { values.put(user.id(), user); }
    @Override public void evict(UserId id) { values.remove(id); }
}
