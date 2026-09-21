package top.egon.cola.archetype.source.light.adapter.user.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.regex.Pattern;

/** Adapter-boundary user rules; the email shape is the part native constraints cannot pin down. */
@Component("userRequestValidator")
@RequiredArgsConstructor
@Slf4j
public class UserRequestValidator extends BaseValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public CreateUserDTO validate(CreateUserDTO request) {
        validateBean(request, NativeRpcValidationGroup.class);
        if (request.email() == null || !EMAIL.matcher(request.email()).matches()) {
            throw new IllegalArgumentException("email must be valid");
        }
        return request;
    }
}
