package ${package}.domain.user.aggregates;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.user.vos.PermissionCode;

import java.util.List;

public final class RolePermissionAggregate {

    private final Role role;

    public RolePermissionAggregate(Role role, List<PermissionCode> permissionCodes) {
        this.role = role.permissionCodes().equals(permissionCodes)
            ? role
            : new Role(role.id(), role.code(), role.name(), role.status(), permissionCodes);
    }

    public void grant(Permission permission) {
        if (role.status() == RoleStatus.ARCHIVED) {
            throw rejected(OrganizationDomainErrorCode.ROLE_ARCHIVED, "archived role cannot receive permissions");
        }
        if (permission.status() == PermissionStatus.INACTIVE) {
            throw rejected(OrganizationDomainErrorCode.PERMISSION_INACTIVE, "inactive permission cannot be granted");
        }
        if (role.permissionCodes().contains(permission.code())) {
            throw rejected(OrganizationDomainErrorCode.DUPLICATE_PERMISSION_GRANT, "permission already granted");
        }
        role.grant(permission.code());
    }

    public Role role() { return role; }
    public List<PermissionCode> permissionCodes() { return role.permissionCodes(); }

    private static OrganizationDomainException rejected(OrganizationDomainErrorCode code, String message) {
        return new OrganizationDomainException(code, message);
    }
}
