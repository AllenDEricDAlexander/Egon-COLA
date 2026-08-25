package ${package}.domain.user.service;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.vos.RoleCode;
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
