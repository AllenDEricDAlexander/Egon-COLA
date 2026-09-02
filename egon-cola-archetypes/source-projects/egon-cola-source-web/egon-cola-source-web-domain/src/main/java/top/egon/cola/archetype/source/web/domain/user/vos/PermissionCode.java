package top.egon.cola.archetype.source.web.domain.user.vos;

import top.egon.cola.archetype.source.web.domain.validators.OrganizationCodeValidator;

public record PermissionCode(String value) {
    public PermissionCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
