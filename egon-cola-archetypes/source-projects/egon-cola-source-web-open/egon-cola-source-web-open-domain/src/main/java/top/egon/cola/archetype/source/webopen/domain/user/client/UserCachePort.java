package top.egon.cola.archetype.source.webopen.domain.user.client;

import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;

import java.util.Optional;

public interface UserCachePort {
    Optional<User> findById(UserId id);
    void put(User user);
    void evict(UserId id);
}
