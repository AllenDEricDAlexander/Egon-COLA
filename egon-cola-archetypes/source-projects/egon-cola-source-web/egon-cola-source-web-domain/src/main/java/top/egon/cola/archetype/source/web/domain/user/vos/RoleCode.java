package top.egon.cola.archetype.source.web.domain.user.vos;

import top.egon.cola.archetype.source.web.domain.validators.OrganizationCodeValidator;

public record RoleCode(String value) {

    public RoleCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
