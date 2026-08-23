package ${package}.domain.user.client;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.UserId;

import java.util.Optional;

public interface UserCachePort {
    Optional<User> findById(UserId id);
    void put(User user);
    void evict(UserId id);
}
