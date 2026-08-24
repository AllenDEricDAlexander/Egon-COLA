package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.UserMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("userRepositoryImpl")
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final UserPOConverter converter;
    private final LongIdGenerator idGenerator;

    @Override
    public User save(User user) {
        try {
            UserPO saved = converter.toPO(user);
            int affected = userMapper.selectById(user.id().value()) == null
                    ? userMapper.insert(saved)
                    : userMapper.updateById(saved);
            requireAffected(affected, "save user");
            user.roleCodes().forEach(roleCode -> Optional.ofNullable(
                    roleMapper.selectByCode(roleCode.value()))
                .ifPresent(role -> saveRoleIfMissing(user.id().value(), role.getId())));
            return restore(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "user persistence conflict", exception);
        }
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(userMapper.selectById(userId.value())).map(this::restore);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return userMapper.countByEmail(normalizedEmail) > 0;
    }

    private void saveRoleIfMissing(Long userId, Long roleId) {
        if (userRoleMapper.countByUserIdAndRoleId(userId, roleId) == 0) {
            int affected = userRoleMapper.insert(
                    new UserRolePO(idGenerator.nextLongId(), userId, roleId, LocalDateTime.now()));
            requireAffected(affected, "insert user role");
        }
    }

    private User restore(UserPO userPO) {
        List<RoleCode> roleCodes = userRoleMapper.selectByUserId(userPO.getId()).stream()
            .map(UserRolePO::getRoleId)
            .map(roleMapper::selectById)
            .filter(java.util.Objects::nonNull)
            .map(role -> new RoleCode(role.getCode()))
            .toList();
        return User.restore(new UserId(userPO.getId()), userPO.getName(), userPO.getEmail(),
            UserStatus.valueOf(userPO.getStatus()), roleCodes);
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
