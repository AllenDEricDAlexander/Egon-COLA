package top.egon.cola.archetype.source.lightopen.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.converter.RolePOConverter;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.dao.RoleDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.dao.RolePermissionDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.RolePO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.RolePermissionPO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.Optional;

/** MyBatis-Plus implementation of the role domain service. */
@Slf4j
@Service("roleDomainService")
@RequiredArgsConstructor
public class RoleDomainServiceImpl
        extends EgonColaServiceImpl<RoleDAO, RolePO>
        implements RoleDomainService<RolePO> {

    @Qualifier("roleDAO")
    private final RoleDAO roleDAO;
    @Qualifier("rolePermissionDAO")
    private final RolePermissionDAO rolePermissionDAO;
    @Qualifier("rolePOConverterImpl")
    private final RolePOConverter converter;
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
    public Optional<Role> findByCode(RoleCode roleCode) {
        return roleDAO.selectByCode(roleCode.value()).stream().findFirst().map(converter::toSource);
    }

    @Override
    @Transactional
    public Role save(Role role) {
        RolePO po = converter.toTarget(role);
        if (po.getId() == null) {
            po.setId(idGenerator.nextLongId());
        }
        roleDAO.insert(po);
        return converter.toSource(po);
    }

    @Override
    @Transactional
    public void savePermissions(RolePermissionAggregate aggregate) {
        aggregate.permissions().forEach(permissionCode -> rolePermissionDAO.insert(
                RolePermissionPO.builder()
                        .roleCode(aggregate.role().code().value())
                        .permissionCode(permissionCode.value())
                        .build()));
    }

    @Override
    public UserAggregate assignRole(UserAggregate user, Role role) {
        user.assign(role);
        return user;
    }
}
