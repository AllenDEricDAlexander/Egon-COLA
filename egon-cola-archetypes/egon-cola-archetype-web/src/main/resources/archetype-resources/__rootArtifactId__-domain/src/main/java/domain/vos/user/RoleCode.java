package ${package}.domain.vos.user;

import ${package}.domain.validators.OrganizationCodeValidator;

public record RoleCode(String value) {

    public RoleCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
