package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.jpa.RoleJpaRepository;
import ${package}.infrastructure.user.repo.jpa.UserRoleJpaRepository;
import ${package}.infrastructure.user.repo.jpa.UserJpaRepository;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("userRepositoryImpl")
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserPOConverter converter;
    private final IdGenerator idGenerator;

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
            userRoleJpaRepository.save(
                    new UserRolePO(idGenerator.nextId(), userId, roleId, LocalDateTime.now()));
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
