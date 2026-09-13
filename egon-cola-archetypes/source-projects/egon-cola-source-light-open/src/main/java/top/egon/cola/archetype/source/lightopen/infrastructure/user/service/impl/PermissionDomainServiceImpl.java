package top.egon.cola.archetype.source.lightopen.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.converter.PermissionPOConverter;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.PermissionRepository;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.RolePermissionRepository;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.UserRoleRepository;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.PermissionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Business rules and orchestration for the permission domain service. */
@Slf4j
@Validated
@Service("permissionDomainService")
@RequiredArgsConstructor
public class PermissionDomainServiceImpl
        implements PermissionDomainService {

    @Qualifier("permissionRepository")
    private final PermissionRepository permissionRepository;
    @Qualifier("userRoleRepository")
    private final UserRoleRepository userRoleRepository;
    @Qualifier("rolePermissionRepository")
    private final RolePermissionRepository rolePermissionRepository;
    @Qualifier("permissionPOConverterImpl")
    private final PermissionPOConverter converter;

    @Override
    public Optional<Permission> findByCode(PermissionCode permissionCode) {
        return permissionRepository.selectByCode(permissionCode.value()).stream()
                .findFirst().map(converter::toSource);
    }

    @Override
    @Transactional
    public Permission save(Permission permission) {
        PermissionPO po = converter.toTarget(permission);
        PermissionPO current = permissionRepository.selectByCode(po.getCode()).stream().findFirst().orElse(null);
        if (current != null) { converter.updateMetadata(po, current); }
        boolean written = current == null ? permissionRepository.save(po) : permissionRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return converter.toSource(po);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<String> roles = userRoleRepository.selectByUserId(userId.value()).stream()
                .map(item -> item.getRoleCode()).distinct().toList();
        if (roles.isEmpty()) {
            return List.of();
        }
        List<String> permissions = rolePermissionRepository.selectByRoleCodeIn(roles).stream()
                .map(item -> item.getPermissionCode()).distinct().toList();
        return permissionRepository.selectByCodeIn(permissions).stream().map(converter::toSource).toList();
    }

    @Override
    public RolePermissionAggregate grantPermission(
            RolePermissionAggregate role, Permission permission) {
        role.grant(permission);
        return role;
    }

}
