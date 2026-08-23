package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.mapper.PermissionMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("permissionRepository")
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionPOConverter converter;

    @Override
    public Optional<Permission> findByCode(PermissionCode permissionCode) {
        return Optional.ofNullable(permissionMapper.selectById(permissionCode.value()))
                .map(converter::toDomain);
    }

    @Override
    public Permission save(Permission permission) {
        var po = converter.toPO(permission);
        int affected = permissionMapper.selectById(po.getCode()) == null
                ? permissionMapper.insert(po)
                : permissionMapper.updateById(po);
        requireAffected(affected, "permission");
        return converter.toDomain(po);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<String> roleCodes = userRoleMapper.findByUserId(userId.value()).stream()
                .map(userRole -> userRole.getRoleCode())
                .distinct()
                .toList();
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        List<String> permissionCodes = rolePermissionMapper.findByRoleCodes(roleCodes).stream()
                .map(rolePermission -> rolePermission.getPermissionCode())
                .distinct()
                .toList();
        if (permissionCodes.isEmpty()) {
            return List.of();
        }
        return permissionMapper.findByCodesOrderByCode(permissionCodes).stream()
                .map(converter::toDomain)
                .toList();
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " persistence affected " + affected
                    + " rows");
        }
    }
}
