package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.dao.PermissionDAO;
import ${package}.infrastructure.user.repo.dao.RoleDAO;
import ${package}.infrastructure.user.repo.dao.RolePermissionDAO;
import ${package}.infrastructure.user.repo.dao.UserDAO;
import ${package}.infrastructure.user.repo.dao.UserRoleDAO;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service("userDomainService")
@RequiredArgsConstructor
public class UserDomainServiceImpl
        extends EgonColaServiceImpl<UserDAO, UserPO>
        implements UserDomainService<UserPO> {

    @Qualifier("userDAO")
    private final UserDAO userDAO;
    @Qualifier("userRoleDAO")
    private final UserRoleDAO userRoleDAO;
    @Qualifier("roleDAO")
    private final RoleDAO roleDAO;
    @Qualifier("rolePermissionDAO")
    private final RolePermissionDAO rolePermissionDAO;
    @Qualifier("permissionDAO")
    private final PermissionDAO permissionDAO;
    @Qualifier("userPOConverter")
    private final UserPOConverter userConverter;
    @Qualifier("rolePOConverter")
    private final RolePOConverter roleConverter;
    @Qualifier("longIdGenerator")
    private final LongIdGenerator idGenerator;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public User create(UserId userId, String name, String email) {
        return new User(userId, name, email, UserStatus.ACTIVE);
    }

    @Override
    public User save(User user) {
        UserPO po = userConverter.toTarget(user);
        UserPO existing = userDAO.selectById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = userDAO.insert(po) == 1;
        } else {
            copyMetadata(existing, po);
            saved = userDAO.updateById(po) == 1;
        }
        if (!saved) {
            throw new IllegalStateException("save user affected zero rows");
        }
        user.roleCodes().forEach(roleCode -> Optional.ofNullable(roleDAO.selectByCode(roleCode.value()))
                .ifPresent(role -> saveRoleIfMissing(po.getId(), role.getId())));
        return restore(po);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(getById(userId.value())).map(this::restore);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return userDAO.countByEmail(normalizedEmail) > 0;
    }

    @Override
    public Optional<Role> findRoleByCode(RoleCode code) {
        return Optional.ofNullable(roleDAO.selectByCode(code.value())).map(this::restoreRole);
    }

    @Override
    public Role saveRole(Role role) {
        RolePO po = roleConverter.toTarget(role);
        saveRoleRow(po);
        role.permissionCodes().forEach(permissionCode ->
                Optional.ofNullable(permissionDAO.selectByCode(permissionCode.value()))
                        .ifPresent(permission -> savePermissionRelationIfMissing(po.getId(), permission.getId())));
        return restoreRole(po);
    }

    private User restore(UserPO userPO) {
        List<RoleCode> roleCodes = userRoleDAO.selectByUserId(userPO.getId()).stream()
                .map(UserRolePO::getRoleId)
                .map(roleDAO::selectById)
                .filter(java.util.Objects::nonNull)
                .map(role -> new RoleCode(role.getCode()))
                .toList();
        return userConverter.toEntity(userPO, roleCodes);
    }

    private Role restoreRole(RolePO rolePO) {
        List<PermissionCode> permissionCodes = rolePermissionDAO.selectByRoleId(rolePO.getId()).stream()
                .map(RolePermissionPO::getPermissionId)
                .map(permissionDAO::selectById)
                .filter(java.util.Objects::nonNull)
                .map(permission -> new PermissionCode(permission.getCode()))
                .toList();
        return roleConverter.toEntity(rolePO, permissionCodes);
    }

    private void saveRoleRow(RolePO rolePO) {
        RolePO existing = rolePO.getId() == null ? null : roleDAO.selectById(rolePO.getId());
        if (existing != null) {
            copyMetadata(existing, rolePO);
        }
        int affected = existing == null ? roleDAO.insert(rolePO) : roleDAO.updateById(rolePO);
        if (affected != 1) {
            throw new IllegalStateException("save role affected zero rows");
        }
    }

    private void saveRoleIfMissing(Long userId, Long roleId) {
        if (userRoleDAO.countByUserIdAndRoleId(userId, roleId) == 0) {
            UserRolePO relation = UserRolePO.builder().userId(userId).roleId(roleId).build();
            relation.setId(idGenerator.nextLongId());
            userRoleDAO.insert(relation);
        }
    }

    private void savePermissionRelationIfMissing(Long roleId, Long permissionId) {
        if (rolePermissionDAO.countByRoleIdAndPermissionId(roleId, permissionId) == 0) {
            RolePermissionPO relation = RolePermissionPO.builder()
                    .roleId(roleId).permissionId(permissionId).build();
            relation.setId(idGenerator.nextLongId());
            rolePermissionDAO.insert(relation);
        }
    }

    private static void copyMetadata(UserPO source, UserPO target) {
        target.setTenantId(source.getTenantId());
        target.setCreateUserId(source.getCreateUserId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateUserId(source.getUpdateUserId());
        target.setUpdateTime(source.getUpdateTime());
        target.setIsDeleted(source.getIsDeleted());
    }

    private static void copyMetadata(RolePO source, RolePO target) {
        target.setTenantId(source.getTenantId());
        target.setCreateUserId(source.getCreateUserId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateUserId(source.getUpdateUserId());
        target.setUpdateTime(source.getUpdateTime());
        target.setIsDeleted(source.getIsDeleted());
    }
}
