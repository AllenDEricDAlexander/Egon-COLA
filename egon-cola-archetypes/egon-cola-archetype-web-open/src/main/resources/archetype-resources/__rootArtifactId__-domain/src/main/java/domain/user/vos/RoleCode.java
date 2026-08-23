package ${package}.domain.user.vos;

import ${package}.domain.validators.OrganizationCodeValidator;

public record RoleCode(String value) {

    public RoleCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
