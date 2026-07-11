package ${package}.infrastructure.repo.user.impl;

import ${package}.domain.entities.user.Role;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.repos.user.RoleRepository;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;
import ${package}.infrastructure.repo.user.converter.RolePOConverter;
import ${package}.infrastructure.repo.user.jpa.PermissionJpaRepository;
import ${package}.infrastructure.repo.user.jpa.RoleJpaRepository;
import ${package}.infrastructure.repo.user.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.repo.user.po.RolePO;
import ${package}.infrastructure.repo.user.po.RolePermissionPO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("roleRepositoryImpl")
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final RolePOConverter converter;

    public RoleRepositoryImpl(
            RoleJpaRepository roleJpaRepository,
            PermissionJpaRepository permissionJpaRepository,
            RolePermissionJpaRepository rolePermissionJpaRepository,
            RolePOConverter converter) {
        this.roleJpaRepository = roleJpaRepository;
        this.permissionJpaRepository = permissionJpaRepository;
        this.rolePermissionJpaRepository = rolePermissionJpaRepository;
        this.converter = converter;
    }

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        return roleJpaRepository.findByCode(code.value()).map(this::restore);
    }

    @Override
    public Role save(Role role) {
        try {
            RolePO saved = roleJpaRepository.save(converter.toPO(role));
            role.permissionCodes().forEach(code -> permissionJpaRepository.findByCode(code.value())
                .ifPresent(permission -> saveRelationIfMissing(saved.getId(), permission.getId())));
            return restore(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "role persistence conflict", exception);
        }
    }

    private Role restore(RolePO rolePO) {
        List<PermissionCode> codes = rolePermissionJpaRepository.findByRoleId(rolePO.getId()).stream()
            .map(RolePermissionPO::getPermissionId)
            .map(permissionJpaRepository::findById)
            .flatMap(Optional::stream)
            .map(permission -> new PermissionCode(permission.getCode()))
            .toList();
        return converter.toEntity(rolePO, codes);
    }

    private void saveRelationIfMissing(String roleId, String permissionId) {
        if (!rolePermissionJpaRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            rolePermissionJpaRepository.save(new RolePermissionPO(roleId, permissionId, LocalDateTime.now()));
        }
    }
}
