package top.egon.cola.archetype.source.light.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.light.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.Permission;
import top.egon.cola.archetype.source.light.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.light.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.light.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.converter.PermissionPOConverter;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.dao.PermissionDAO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.dao.RolePermissionDAO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.dao.UserRoleDAO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.PermissionPO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.List;
import java.util.Optional;

/** MyBatis-Plus implementation of the permission domain service. */
@Slf4j
@Service("permissionDomainService")
@RequiredArgsConstructor
public class PermissionDomainServiceImpl
        extends EgonColaServiceImpl<PermissionDAO, PermissionPO>
        implements PermissionDomainService<PermissionPO> {

    @Qualifier("permissionDAO")
    private final PermissionDAO permissionDAO;
    @Qualifier("userRoleDAO")
    private final UserRoleDAO userRoleDAO;
    @Qualifier("rolePermissionDAO")
    private final RolePermissionDAO rolePermissionDAO;
    @Qualifier("permissionPOConverterImpl")
    private final PermissionPOConverter converter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public Optional<Permission> findByCode(PermissionCode permissionCode) {
        return permissionDAO.selectByCode(permissionCode.value()).stream()
                .findFirst().map(converter::toSource);
    }

    @Override
    public Permission save(Permission permission) {
        PermissionPO po = converter.toTarget(permission);
        po.setId(idGenerator.nextLongId());
        permissionDAO.insert(po);
        return converter.toSource(po);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<String> roles = userRoleDAO.selectByUserId(userId.value()).stream()
                .map(item -> item.getRoleCode()).distinct().toList();
        if (roles.isEmpty()) {
            return List.of();
        }
        List<String> permissions = rolePermissionDAO.selectByRoleCodeIn(roles).stream()
                .map(item -> item.getPermissionCode()).distinct().toList();
        return permissionDAO.selectByCodeIn(permissions).stream().map(converter::toSource).toList();
    }

    @Override
    public RolePermissionAggregate grantPermission(
            RolePermissionAggregate role, Permission permission) {
        role.grant(permission);
        return role;
    }
}
