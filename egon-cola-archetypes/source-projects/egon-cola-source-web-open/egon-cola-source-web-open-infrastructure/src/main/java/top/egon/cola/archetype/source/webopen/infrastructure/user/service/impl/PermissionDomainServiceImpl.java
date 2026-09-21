package top.egon.cola.archetype.source.webopen.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.webopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.webopen.infrastructure.user.converter.PermissionPOConverter;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.PermissionRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.RolePermissionRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.UserRoleRepository;
import top.egon.cola.archetype.source.webopen.infrastructure.user.po.PermissionPO;
import top.egon.cola.archetype.source.webopen.infrastructure.user.po.RolePermissionPO;
import top.egon.cola.archetype.source.webopen.infrastructure.user.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public void grant(RolePermissionAggregate aggregate, Permission permission) {
        aggregate.grant(permission);
    }

    @Override
    public Optional<Permission> findByCode(PermissionCode code) {
        return Optional.ofNullable(permissionRepository.selectByCode(code.value())).map(converter::toSource);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<Long> roleIds = userRoleRepository.selectByUserId(userId.value()).stream()
                .map(UserRolePO::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionRepository.selectByRoleIds(roleIds).stream()
                .map(RolePermissionPO::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionRepository.selectPermissionsByIds(permissionIds).stream()
                .map(converter::toSource).toList();
    }

    @Override
    @Transactional
    public Permission save(Permission permission) {
        PermissionPO po = converter.toTarget(permission);
        PermissionPO existing = permissionRepository.getById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = permissionRepository.save(po);
        } else {
            converter.updateMetadata(po, existing);
            saved = permissionRepository.updateById(po);
        }
        if (!saved) {
            throw new IllegalStateException("save permission affected zero rows");
        }
        return converter.toSource(po);
    }

}
