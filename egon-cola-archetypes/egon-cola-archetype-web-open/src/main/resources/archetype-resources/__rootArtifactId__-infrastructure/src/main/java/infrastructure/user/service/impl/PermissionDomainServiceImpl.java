package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.dao.PermissionDAO;
import ${package}.infrastructure.user.repo.dao.RolePermissionDAO;
import ${package}.infrastructure.user.repo.dao.UserRoleDAO;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.List;
import java.util.Optional;

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
    @Qualifier("permissionPOConverter")
    private final PermissionPOConverter converter;
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
    public void grant(RolePermissionAggregate aggregate, Permission permission) {
        aggregate.grant(permission);
    }

    @Override
    public Optional<Permission> findByCode(PermissionCode code) {
        return Optional.ofNullable(permissionDAO.selectByCode(code.value())).map(converter::toSource);
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        List<Long> roleIds = userRoleDAO.selectByUserId(userId.value()).stream()
                .map(UserRolePO::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionDAO.selectByRoleIds(roleIds).stream()
                .map(RolePermissionPO::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionDAO.selectPermissionsByIds(permissionIds).stream()
                .map(converter::toSource).toList();
    }

    @Override
    public Permission save(Permission permission) {
        PermissionPO po = converter.toTarget(permission);
        PermissionPO existing = permissionDAO.selectById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = permissionDAO.insert(po) == 1;
        } else {
            copyMetadata(existing, po);
            saved = permissionDAO.updateById(po) == 1;
        }
        if (!saved) {
            throw new IllegalStateException("save permission affected zero rows");
        }
        return converter.toSource(po);
    }

    private static void copyMetadata(PermissionPO source, PermissionPO target) {
        target.setTenantId(source.getTenantId());
        target.setCreateUserId(source.getCreateUserId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateUserId(source.getUpdateUserId());
        target.setUpdateTime(source.getUpdateTime());
        target.setIsDeleted(source.getIsDeleted());
    }
}
