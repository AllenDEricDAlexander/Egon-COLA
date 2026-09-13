package top.egon.cola.archetype.source.web.domain.user.service;

import top.egon.cola.archetype.source.web.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.web.domain.user.entities.Permission;
import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;

import java.util.List;
import java.util.Optional;

public interface PermissionDomainService {
    void grant( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull RolePermissionAggregate aggregate, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Permission permission);

    Optional<Permission> findByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull PermissionCode code);

    List<Permission> findByUserId( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull UserId userId);

    Permission save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Permission permission);
}
