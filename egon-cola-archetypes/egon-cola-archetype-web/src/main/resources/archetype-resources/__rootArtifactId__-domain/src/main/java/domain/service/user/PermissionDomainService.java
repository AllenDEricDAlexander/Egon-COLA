package ${package}.domain.service.user;

import ${package}.domain.aggregates.user.RolePermissionAggregate;
import ${package}.domain.entities.user.Permission;

public interface PermissionDomainService {
    void grant(RolePermissionAggregate aggregate, Permission permission);
}
