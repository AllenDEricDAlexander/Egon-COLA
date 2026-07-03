package ${package}.domain.repos.user;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(String userId);

    Page<User> findPage(int currentPage, int pageSize);

    boolean existsByEmail(String email);
}
