package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.jpa.RoleJpaRepository;
import ${package}.infrastructure.user.repo.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository("roleRepository")
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final RolePOConverter converter;

    @Override
    public Optional<Role> findByCode(RoleCode roleCode) {
        return roleJpaRepository.findById(roleCode.value()).map(converter::toDomain);
    }

    @Override
    public Role save(Role role) {
        return converter.toDomain(roleJpaRepository.save(converter.toPO(role)));
    }

    @Override
    public void savePermissions(RolePermissionAggregate aggregate) {
        aggregate.permissions().forEach(permissionCode -> rolePermissionJpaRepository.save(
                new RolePermissionPO(
                        aggregate.role().code().value(), permissionCode.value(), Instant.now())));
    }
}
