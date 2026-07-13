package ${package}.domain.user.repos;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    default Optional<User> findById(String userId) {
        return findById(new UserId(userId));
    }

    boolean existsByEmail(String normalizedEmail);
}
