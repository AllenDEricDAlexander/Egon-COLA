package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository("roleRepository")
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RolePOConverter converter;

    @Override
    public Optional<Role> findByCode(RoleCode roleCode) {
        return Optional.ofNullable(roleMapper.selectById(roleCode.value())).map(converter::toDomain);
    }

    @Override
    public Role save(Role role) {
        var po = converter.toPO(role);
        int affected = roleMapper.selectById(po.getCode()) == null
                ? roleMapper.insert(po)
                : roleMapper.updateById(po);
        requireAffected(affected, "role");
        return converter.toDomain(po);
    }

    @Override
    public void savePermissions(RolePermissionAggregate aggregate) {
        aggregate.permissions().forEach(permissionCode -> requireAffected(
                rolePermissionMapper.insertRelation(new RolePermissionPO(
                        aggregate.role().code().value(), permissionCode.value(), Instant.now())),
                "role permission"));
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " persistence affected " + affected
                    + " rows");
        }
    }
}
