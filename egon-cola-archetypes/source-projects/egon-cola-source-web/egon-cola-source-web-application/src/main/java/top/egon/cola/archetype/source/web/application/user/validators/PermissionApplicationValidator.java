package top.egon.cola.archetype.source.web.application.user.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Role and permission code guard; the value objects own the accepted code shape. */
@RequiredArgsConstructor
@Slf4j
public class PermissionApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public RoleCode roleCode(String value) {
        return new RoleCode(value);
    }

    public PermissionCode permissionCode(String value) {
        return new PermissionCode(value);
    }
}
