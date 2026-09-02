package top.egon.cola.archetype.source.webopen.domain.user.vos;

import top.egon.cola.archetype.source.webopen.domain.validators.OrganizationCodeValidator;

public record RoleCode(String value) {

    public RoleCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
