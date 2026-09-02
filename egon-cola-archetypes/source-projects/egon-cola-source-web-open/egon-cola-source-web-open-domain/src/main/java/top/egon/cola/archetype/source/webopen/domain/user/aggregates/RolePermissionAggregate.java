package top.egon.cola.archetype.source.webopen.domain.user.aggregates;

import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainException;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;

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
