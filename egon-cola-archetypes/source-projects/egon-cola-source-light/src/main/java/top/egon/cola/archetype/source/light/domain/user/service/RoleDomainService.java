package top.egon.cola.archetype.source.light.domain.user.service;

import top.egon.cola.archetype.source.light.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.light.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

/** Persistence-owning role domain service contract. */
public interface RoleDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    Optional<Role> findByCode(RoleCode roleCode);

    Role save(Role role);

    void savePermissions(RolePermissionAggregate aggregate);

    UserAggregate assignRole(UserAggregate user, Role role);
}
