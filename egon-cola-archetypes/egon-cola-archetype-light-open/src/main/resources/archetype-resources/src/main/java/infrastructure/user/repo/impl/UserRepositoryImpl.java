package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.mapper.UserMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository("userRepository")
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserPOConverter converter;

    @Override
    public User save(User user) {
        var po = converter.toPO(user);
        int affected = userMapper.selectById(po.getId()) == null
                ? userMapper.insert(po)
                : userMapper.updateById(po);
        requireAffected(affected, "user");
        return converter.toDomain(po);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(userMapper.selectById(userId.value())).map(converter::toDomain);
    }

    @Override
    public void saveRoles(UserAggregate aggregate) {
        aggregate.roles().forEach(roleCode -> requireAffected(
                userRoleMapper.insertRelation(new UserRolePO(
                        aggregate.user().id().value(), roleCode.value(), Instant.now())),
                "user role"));
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " persistence affected " + affected
                    + " rows");
        }
    }
}
