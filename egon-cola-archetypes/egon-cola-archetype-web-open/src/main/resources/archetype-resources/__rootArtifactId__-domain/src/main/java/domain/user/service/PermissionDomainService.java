package ${package}.domain.user.service;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;

public interface PermissionDomainService {
    void grant(RolePermissionAggregate aggregate, Permission permission);
}
