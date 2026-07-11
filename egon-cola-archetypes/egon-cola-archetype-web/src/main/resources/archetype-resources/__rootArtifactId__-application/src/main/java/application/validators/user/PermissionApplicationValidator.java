package ${package}.application.validators.user;

import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;

public final class PermissionApplicationValidator {
    public RoleCode roleCode(String value) { return new RoleCode(value); }
    public PermissionCode permissionCode(String value) { return new PermissionCode(value); }
}
