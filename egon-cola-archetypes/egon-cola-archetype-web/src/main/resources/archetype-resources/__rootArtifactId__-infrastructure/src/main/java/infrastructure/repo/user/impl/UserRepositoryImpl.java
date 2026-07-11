package ${package}.infrastructure.repo.user.impl;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.vos.user.UserId;
import ${package}.domain.vos.user.RoleCode;
import ${package}.infrastructure.repo.user.converter.UserPOConverter;
import ${package}.infrastructure.repo.user.jpa.RoleJpaRepository;
import ${package}.infrastructure.repo.user.jpa.UserRoleJpaRepository;
import ${package}.infrastructure.repo.user.jpa.UserJpaRepository;
import ${package}.infrastructure.repo.user.po.UserPO;
import ${package}.infrastructure.repo.user.po.UserRolePO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("userRepositoryImpl")
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserPOConverter converter;

    public UserRepositoryImpl(
            UserJpaRepository userJpaRepository,
            UserRoleJpaRepository userRoleJpaRepository,
            RoleJpaRepository roleJpaRepository,
            UserPOConverter converter) {
        this.userJpaRepository = userJpaRepository;
        this.userRoleJpaRepository = userRoleJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.converter = converter;
    }

    @Override
    public User save(User user) {
        try {
            UserPO saved = userJpaRepository.save(converter.toPO(user));
            user.roleCodes().forEach(roleCode -> roleJpaRepository.findByCode(roleCode.value())
                .ifPresent(role -> saveRoleIfMissing(user.id().value(), role.getId())));
            return restore(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "user persistence conflict", exception);
        }
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findById(userId.value()).map(this::restore);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return userJpaRepository.existsByEmail(normalizedEmail);
    }

    private void saveRoleIfMissing(String userId, String roleId) {
        if (!userRoleJpaRepository.existsByUserIdAndRoleId(userId, roleId)) {
            userRoleJpaRepository.save(new UserRolePO(userId, roleId, LocalDateTime.now()));
        }
    }

    private User restore(UserPO userPO) {
        List<RoleCode> roleCodes = userRoleJpaRepository.findByUserId(userPO.getId()).stream()
            .map(UserRolePO::getRoleId)
            .map(roleJpaRepository::findById)
            .flatMap(Optional::stream)
            .map(role -> new RoleCode(role.getCode()))
            .toList();
        return User.restore(new UserId(userPO.getId()), userPO.getName(), userPO.getEmail(),
            UserStatus.valueOf(userPO.getStatus()), roleCodes);
    }
}
