package top.egon.cola.archetype.source.webopen.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.converter.RolePOConverter;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.converter.UserPOConverter;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.PermissionRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.RoleRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.RolePermissionRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.UserRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.UserRoleRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.RolePO;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.RolePermissionPO;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.UserPO;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.util.List;
import java.util.Optional;

@Slf4j
@Validated
@Service("userDomainService")
@RequiredArgsConstructor
public class UserDomainServiceImpl
        implements UserDomainService {

    @Qualifier("userRepository")
    private final UserRepository userRepository;
    @Qualifier("userRoleRepository")
    private final UserRoleRepository userRoleRepository;
    @Qualifier("roleRepository")
    private final RoleRepository roleRepository;
    @Qualifier("rolePermissionRepository")
    private final RolePermissionRepository rolePermissionRepository;
    @Qualifier("permissionRepository")
    private final PermissionRepository permissionRepository;
    @Qualifier("userPOConverterImpl")
    private final UserPOConverter userConverter;
    @Qualifier("rolePOConverterImpl")
    private final RolePOConverter roleConverter;

    @Override
    public User create(UserId userId, String name, String email) {
        return new User(userId, name, email, UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserPO po = userConverter.toTarget(user);
        UserPO existing = userRepository.getById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = userRepository.save(po);
        } else {
            userConverter.updateMetadata(po, existing);
            saved = userRepository.updateById(po);
        }
        if (!saved) {
            throw new IllegalStateException("save user affected zero rows");
        }
        user.roleCodes().forEach(roleCode -> Optional.ofNullable(roleRepository.selectByCode(roleCode.value()))
                .ifPresent(role -> saveRoleIfMissing(po.getId(), role.getId())));
        return restore(po);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(userRepository.getById(userId.value())).map(this::restore);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return userRepository.countByEmail(normalizedEmail) > 0;
    }

    @Override
    public Optional<Role> findRoleByCode(RoleCode code) {
        return Optional.ofNullable(roleRepository.selectByCode(code.value())).map(this::restoreRole);
    }

    @Override
    @Transactional
    public Role saveRole(Role role) {
        RolePO po = roleConverter.toTarget(role);
        saveRoleRow(po);
        role.permissionCodes().forEach(permissionCode ->
                Optional.ofNullable(permissionRepository.selectByCode(permissionCode.value()))
                        .ifPresent(permission -> savePermissionRelationIfMissing(po.getId(), permission.getId())));
        return restoreRole(po);
    }

    private User restore(UserPO userPO) {
        List<RoleCode> roleCodes = userRoleRepository.selectByUserId(userPO.getId()).stream()
                .map(UserRolePO::getRoleId)
                .map(roleRepository::getById)
                .filter(java.util.Objects::nonNull)
                .map(role -> new RoleCode(role.getCode()))
                .toList();
        return userConverter.toEntity(userPO, roleCodes);
    }

    private Role restoreRole(RolePO rolePO) {
        List<PermissionCode> permissionCodes = rolePermissionRepository.selectByRoleId(rolePO.getId()).stream()
                .map(RolePermissionPO::getPermissionId)
                .map(permissionRepository::getById)
                .filter(java.util.Objects::nonNull)
                .map(permission -> new PermissionCode(permission.getCode()))
                .toList();
        return roleConverter.toEntity(rolePO, permissionCodes);
    }

    private void saveRoleRow(RolePO rolePO) {
        RolePO existing = rolePO.getId() == null ? null : roleRepository.getById(rolePO.getId());
        if (existing != null) {
            roleConverter.updateMetadata(rolePO, existing);
        }
        boolean affected = existing == null ? roleRepository.save(rolePO) : roleRepository.updateById(rolePO);
        if (!affected) {
            throw new IllegalStateException("save role affected zero rows");
        }
    }

    private void saveRoleIfMissing(Long userId, Long roleId) {
        if (userRoleRepository.countByUserIdAndRoleId(userId, roleId) == 0) {
            UserRolePO relation = UserRolePO.builder().userId(userId).roleId(roleId).build();
            relation.setId(SnowflakeIdGenerator.nextLongId());
            if (!userRoleRepository.save(relation)) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        }
    }

    private void savePermissionRelationIfMissing(Long roleId, Long permissionId) {
        if (rolePermissionRepository.countByRoleIdAndPermissionId(roleId, permissionId) == 0) {
            RolePermissionPO relation = RolePermissionPO.builder()
                    .roleId(roleId).permissionId(permissionId).build();
            relation.setId(SnowflakeIdGenerator.nextLongId());
            if (!rolePermissionRepository.save(relation)) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        }
    }

}
