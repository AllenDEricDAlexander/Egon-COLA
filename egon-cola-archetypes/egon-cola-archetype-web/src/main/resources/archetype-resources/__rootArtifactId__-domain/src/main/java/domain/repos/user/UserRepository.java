package ${package}.domain.repos.user;

import ${package}.domain.entities.user.User;
import ${package}.domain.vos.user.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    default Optional<User> findById(String userId) {
        return findById(new UserId(userId));
    }

    boolean existsByEmail(String normalizedEmail);
}
