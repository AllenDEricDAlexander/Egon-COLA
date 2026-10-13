package top.egon.cola.archetype.source.web.application.user.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.web.domain.user.validators.UserDomainValidator;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Organization use-case normalization and validation; it never writes Redis, MQ or persistence state. */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class UserApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public String normalizedEmail(String email) {
        return UserDomainValidator.normalizeEmail(email);
    }
}
