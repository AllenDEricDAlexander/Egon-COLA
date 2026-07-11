package ${package}.domain.repos.user;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import ${package}.domain.vos.user.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    default Optional<User> findById(String userId) {
        return findById(new UserId(userId));
    }

    Page<User> findPage(int currentPage, int pageSize);

    boolean existsByEmail(String normalizedEmail);
}
