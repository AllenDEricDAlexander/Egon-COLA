package top.egon.cola.archetype.source.light.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.light.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.light.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.light.infrastructure.user.converter.RolePOConverter;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.RoleRepository;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.RolePermissionRepository;
import top.egon.cola.archetype.source.light.infrastructure.user.po.RolePO;
import top.egon.cola.archetype.source.light.infrastructure.user.po.RolePermissionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Business rules and orchestration for the role domain service. */
@Slf4j
@Validated
@Service("roleDomainService")
@RequiredArgsConstructor
public class RoleDomainServiceImpl
        implements RoleDomainService {

    @Qualifier("roleRepository")
    private final RoleRepository roleRepository;
    @Qualifier("rolePermissionRepository")
    private final RolePermissionRepository rolePermissionRepository;
    @Qualifier("rolePOConverterImpl")
    private final RolePOConverter converter;

    @Override
    public Optional<Role> findByCode(RoleCode roleCode) {
        return roleRepository.selectByCode(roleCode.value()).stream().findFirst().map(converter::toSource);
    }

    @Override
    @Transactional
    public Role save(Role role) {
        RolePO po = converter.toTarget(role);
        RolePO current = roleRepository.selectByCode(po.getCode()).stream().findFirst().orElse(null);
        if (current != null) { converter.updateMetadata(po, current); }
        boolean written = current == null ? roleRepository.save(po) : roleRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return converter.toSource(po);
    }

    @Override
    @Transactional
    public void savePermissions(RolePermissionAggregate aggregate) {
        aggregate.permissions().forEach(permissionCode -> {
            if (!rolePermissionRepository.save(
                RolePermissionPO.builder()
                        .roleCode(aggregate.role().code().value())
                        .permissionCode(permissionCode.value())
                        .build())) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        });
    }

    @Override
    public UserAggregate assignRole(UserAggregate user, Role role) {
        user.assign(role);
        return user;
    }

}
