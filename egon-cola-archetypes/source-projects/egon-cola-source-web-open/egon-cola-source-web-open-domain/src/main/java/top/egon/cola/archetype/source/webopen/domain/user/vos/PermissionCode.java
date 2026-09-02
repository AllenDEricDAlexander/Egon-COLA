package top.egon.cola.archetype.source.webopen.domain.user.vos;

import top.egon.cola.archetype.source.webopen.domain.validators.OrganizationCodeValidator;

public record PermissionCode(String value) {
    public PermissionCode {
        value = OrganizationCodeValidator.normalize(value);
    }
}
