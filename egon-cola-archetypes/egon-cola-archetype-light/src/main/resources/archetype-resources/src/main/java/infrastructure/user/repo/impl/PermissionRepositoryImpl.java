package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.jpa.PermissionJpaRepository;
import ${package}.infrastructure.user.repo.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.user.repo.jpa.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("permissionRepository")
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionJpaRepository permissionJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final PermissionPOConverter converter;

    @Override
    public Optional<Permission> findByCode(PermissionCode permissionCode) {
        return permissionJpaRepository.findById(permissionCode.value()).map(converter::toDomain);
    }

    @Override
    public Permission save(Permission permission) {
        return converter.toDomain(permissionJpaRepository.save(converter.toPO(permission)));
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<String> roleCodes = userRoleJpaRepository.findByUserId(userId.value()).stream()
                .map(userRole -> userRole.getRoleCode())
                .distinct()
                .toList();
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        List<String> permissionCodes = rolePermissionJpaRepository.findByRoleCodeIn(roleCodes).stream()
                .map(rolePermission -> rolePermission.getPermissionCode())
                .distinct()
                .toList();
        if (permissionCodes.isEmpty()) {
            return List.of();
        }
        return permissionJpaRepository.findByCodeInOrderByCode(permissionCodes).stream()
                .map(converter::toDomain)
                .toList();
    }
}
