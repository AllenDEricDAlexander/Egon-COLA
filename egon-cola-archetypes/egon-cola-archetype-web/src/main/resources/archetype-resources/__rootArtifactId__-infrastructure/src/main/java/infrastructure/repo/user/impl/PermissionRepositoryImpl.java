package ${package}.infrastructure.repo.user.impl;

import ${package}.domain.entities.user.Permission;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.repos.user.PermissionRepository;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.UserId;
import ${package}.infrastructure.repo.user.converter.PermissionPOConverter;
import ${package}.infrastructure.repo.user.jpa.PermissionJpaRepository;
import ${package}.infrastructure.repo.user.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.repo.user.jpa.UserRoleJpaRepository;
import ${package}.infrastructure.repo.user.po.RolePermissionPO;
import ${package}.infrastructure.repo.user.po.UserRolePO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("permissionRepositoryImpl")
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionJpaRepository permissionJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final PermissionPOConverter converter;

    public PermissionRepositoryImpl(
            PermissionJpaRepository permissionJpaRepository,
            UserRoleJpaRepository userRoleJpaRepository,
            RolePermissionJpaRepository rolePermissionJpaRepository,
            PermissionPOConverter converter) {
        this.permissionJpaRepository = permissionJpaRepository;
        this.userRoleJpaRepository = userRoleJpaRepository;
        this.rolePermissionJpaRepository = rolePermissionJpaRepository;
        this.converter = converter;
    }

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
