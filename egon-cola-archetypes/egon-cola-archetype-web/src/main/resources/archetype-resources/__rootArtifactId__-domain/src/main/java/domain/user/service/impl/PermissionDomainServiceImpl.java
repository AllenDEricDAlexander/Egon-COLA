package ${package}.domain.user.service.impl;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.service.PermissionDomainService;

public final class PermissionDomainServiceImpl implements PermissionDomainService {
    @Override
    public void grant(RolePermissionAggregate aggregate, Permission permission) {
        aggregate.grant(permission);
    }
}
