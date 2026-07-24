package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Role;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.jpa.PermissionJpaRepository;
import ${package}.infrastructure.user.repo.jpa.RoleJpaRepository;
import ${package}.infrastructure.user.repo.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("roleRepositoryImpl")
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final RolePOConverter converter;
    private final IdGenerator idGenerator;

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
            rolePermissionJpaRepository.save(new RolePermissionPO(
                    idGenerator.nextId(), roleId, permissionId, LocalDateTime.now()));
        }
    }
}
