package ${package}.domain.service.user.impl;

import ${package}.domain.aggregates.user.RolePermissionAggregate;
import ${package}.domain.entities.user.Permission;
import ${package}.domain.service.user.PermissionDomainService;

public final class PermissionDomainServiceImpl implements PermissionDomainService {
    @Override
    public void grant(RolePermissionAggregate aggregate, Permission permission) {
        aggregate.grant(permission);
    }
}
