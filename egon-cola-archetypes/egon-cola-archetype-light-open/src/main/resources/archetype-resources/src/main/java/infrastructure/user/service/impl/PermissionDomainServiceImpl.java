package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.service.PermissionDomainService;
import org.springframework.stereotype.Service;

@Service("permissionDomainService")
public class PermissionDomainServiceImpl implements PermissionDomainService {
    @Override
    public RolePermissionAggregate grantPermission(
            RolePermissionAggregate role, Permission permission) {
        role.grant(permission);
        return role;
    }
}
