package top.egon.fable-web.domain.repos.user;

import top.egon.fable-web.domain.entities.user.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(String userId);
    boolean existsByEmail(String email);
}
