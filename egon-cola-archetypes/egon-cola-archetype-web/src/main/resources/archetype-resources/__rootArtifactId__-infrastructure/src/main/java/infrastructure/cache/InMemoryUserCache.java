package ${package}.infrastructure.cache;

import ${package}.domain.client.user.UserCachePort;
import ${package}.domain.entities.user.User;
import ${package}.domain.vos.user.UserId;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserCache implements UserCachePort {
    private final ConcurrentHashMap<UserId, User> values = new ConcurrentHashMap<>();
    @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(values.get(id)); }
    @Override public void put(User user) { values.put(user.id(), user); }
    @Override public void evict(UserId id) { values.remove(id); }
}
