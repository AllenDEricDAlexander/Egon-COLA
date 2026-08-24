package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Role;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.mapper.PermissionMapper;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("roleRepositoryImpl")
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RolePOConverter converter;
    private final LongIdGenerator idGenerator;

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        return Optional.ofNullable(roleMapper.selectByCode(code.value())).map(this::restore);
    }

    @Override
    public Role save(Role role) {
        try {
            RolePO saved = converter.toPO(role);
            int affected = roleMapper.selectById(role.id()) == null
                    ? roleMapper.insert(saved)
                    : roleMapper.updateById(saved);
            requireAffected(affected, "save role");
            role.permissionCodes().forEach(code -> Optional.ofNullable(
                    permissionMapper.selectByCode(code.value()))
                .ifPresent(permission -> saveRelationIfMissing(saved.getId(), permission.getId())));
            return restore(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "role persistence conflict", exception);
        }
    }

    private Role restore(RolePO rolePO) {
        List<Long> permissionIds = rolePermissionMapper.selectByRoleId(rolePO.getId()).stream()
            .map(RolePermissionPO::getPermissionId)
            .distinct().toList();
        List<PermissionCode> codes = permissionIds.isEmpty() ? List.of()
            : permissionMapper.selectPermissionsByIds(permissionIds).stream()
            .map(permission -> new PermissionCode(permission.getCode()))
            .toList();
        return converter.toEntity(rolePO, codes);
    }

    private void saveRelationIfMissing(Long roleId, Long permissionId) {
        if (rolePermissionMapper.countByRoleIdAndPermissionId(roleId, permissionId) == 0) {
            int affected = rolePermissionMapper.insert(new RolePermissionPO(
                    idGenerator.nextLongId(), roleId, permissionId, LocalDateTime.now()));
            requireAffected(affected, "insert role permission");
        }
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
