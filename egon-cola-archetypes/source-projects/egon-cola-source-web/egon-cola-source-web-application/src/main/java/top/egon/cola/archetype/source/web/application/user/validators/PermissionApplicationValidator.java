package top.egon.cola.archetype.source.web.application.user.validators;

import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;

public final class PermissionApplicationValidator {
    public RoleCode roleCode(String value) { return new RoleCode(value); }
    public PermissionCode permissionCode(String value) { return new PermissionCode(value); }
}
