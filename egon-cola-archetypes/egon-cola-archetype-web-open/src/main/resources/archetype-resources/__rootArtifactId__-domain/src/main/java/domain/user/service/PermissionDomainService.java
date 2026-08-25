package ${package}.domain.user.service;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.List;
import java.util.Optional;

public interface PermissionDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    void grant(RolePermissionAggregate aggregate, Permission permission);

    Optional<Permission> findByCode(PermissionCode code);

    List<Permission> findByUserId(UserId userId);

    Permission save(Permission permission);
}
