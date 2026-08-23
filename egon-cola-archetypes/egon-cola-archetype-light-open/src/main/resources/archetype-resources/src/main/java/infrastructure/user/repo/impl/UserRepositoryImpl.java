package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.jpa.UserJpaRepository;
import ${package}.infrastructure.user.repo.jpa.UserRoleJpaRepository;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository("userRepository")
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final UserPOConverter converter;

    @Override
    public User save(User user) {
        return converter.toDomain(userJpaRepository.save(converter.toPO(user)));
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findById(userId.value()).map(converter::toDomain);
    }

    @Override
    public void saveRoles(UserAggregate aggregate) {
        aggregate.roles().forEach(roleCode -> userRoleJpaRepository.save(new UserRolePO(
                aggregate.user().id().value(), roleCode.value(), Instant.now())));
    }
}
