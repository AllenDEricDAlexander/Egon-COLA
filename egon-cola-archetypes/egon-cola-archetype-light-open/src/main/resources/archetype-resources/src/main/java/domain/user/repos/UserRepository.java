package ${package}.domain.user.repos;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.UserId;

import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(UserId userId);

    void saveRoles(UserAggregate aggregate);
}
