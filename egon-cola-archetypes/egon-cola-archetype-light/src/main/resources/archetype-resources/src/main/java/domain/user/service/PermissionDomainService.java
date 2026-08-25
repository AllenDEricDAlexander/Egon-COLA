package ${package}.domain.user.service;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.List;
import java.util.Optional;

/** Persistence-owning permission domain service contract. */
public interface PermissionDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    Optional<Permission> findByCode(PermissionCode permissionCode);

    Permission save(Permission permission);

    List<Permission> findByUserId(UserId userId);

    RolePermissionAggregate grantPermission(RolePermissionAggregate role, Permission permission);
}
