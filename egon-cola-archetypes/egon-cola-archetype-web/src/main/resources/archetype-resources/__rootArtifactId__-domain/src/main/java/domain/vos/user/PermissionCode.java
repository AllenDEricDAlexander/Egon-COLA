package ${package}.domain.vos.user;

import ${package}.domain.validators.OrganizationCodeValidator;

public record PermissionCode(String value) {
    public PermissionCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
