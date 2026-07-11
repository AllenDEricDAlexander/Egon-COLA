package ${package}.domain.client.user;

import ${package}.domain.entities.user.User;
import ${package}.domain.vos.user.UserId;

import java.util.Optional;

public interface UserCachePort {
    Optional<User> findById(UserId id);
    void put(User user);
    void evict(UserId id);
}
