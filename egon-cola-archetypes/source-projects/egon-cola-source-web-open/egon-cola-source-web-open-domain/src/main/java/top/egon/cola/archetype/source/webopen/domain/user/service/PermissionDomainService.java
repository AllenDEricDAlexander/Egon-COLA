package top.egon.cola.archetype.source.webopen.domain.user.service;

import top.egon.cola.archetype.source.webopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
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
