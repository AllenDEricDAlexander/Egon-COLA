package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.mapper.PermissionMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("permissionRepositoryImpl")
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionPOConverter converter;

    @Override
    public Optional<Permission> findByCode(PermissionCode code) {
        return Optional.ofNullable(permissionMapper.selectByCode(code.value()))
                .map(converter::toEntity);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<Long> roleIds = userRoleMapper.selectByUserId(userId.value()).stream()
            .map(UserRolePO::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectByRoleIds(roleIds).stream()
            .map(RolePermissionPO::getPermissionId)
            .distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectPermissionsByIds(permissionIds).stream()
            .map(converter::toEntity)
            .toList();
    }

    @Override
    public Permission save(Permission permission) {
        try {
            var po = converter.toPO(permission);
            int affected = permissionMapper.selectById(permission.id()) == null
                    ? permissionMapper.insert(po)
                    : permissionMapper.updateById(po);
            requireAffected(affected, "save permission");
            return converter.toEntity(po);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "permission persistence conflict", exception);
        }
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
