package top.egon.cola.archetype.source.light.domain.user.service;

import top.egon.cola.archetype.source.light.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.Permission;
import top.egon.cola.archetype.source.light.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Persistence-owning permission domain service contract. */
public interface PermissionDomainService {
    Optional<Permission> findByCode( @Valid @NotNull PermissionCode permissionCode);

    Permission save( @Valid @NotNull Permission permission);

    List<Permission> findByUserId( @Valid @NotNull UserId userId);

    RolePermissionAggregate grantPermission( @Valid @NotNull RolePermissionAggregate role, @Valid @NotNull Permission permission);
}
