package ${package}.application.user.validators;

import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;

public final class PermissionApplicationValidator {
    public RoleCode roleCode(String value) { return new RoleCode(value); }
    public PermissionCode permissionCode(String value) { return new PermissionCode(value); }
}
