package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.jpa.PermissionJpaRepository;
import ${package}.infrastructure.user.repo.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.user.repo.jpa.UserRoleJpaRepository;
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
    private final PermissionJpaRepository permissionJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final PermissionPOConverter converter;

    @Override
    public Optional<Permission> findByCode(PermissionCode code) {
        return permissionJpaRepository.findByCode(code.value()).map(converter::toEntity);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<String> roleIds = userRoleJpaRepository.findByUserId(userId.value()).stream()
            .map(UserRolePO::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return rolePermissionJpaRepository.findByRoleIdIn(roleIds).stream()
            .map(RolePermissionPO::getPermissionId)
            .distinct()
            .map(permissionJpaRepository::findById)
            .flatMap(Optional::stream)
            .map(converter::toEntity)
            .toList();
    }

    @Override
    public Permission save(Permission permission) {
        try {
            return converter.toEntity(permissionJpaRepository.save(converter.toPO(permission)));
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "permission persistence conflict", exception);
        }
    }
}
