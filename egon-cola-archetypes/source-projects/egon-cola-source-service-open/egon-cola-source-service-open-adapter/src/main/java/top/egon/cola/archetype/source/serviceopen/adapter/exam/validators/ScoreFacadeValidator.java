package top.egon.cola.archetype.source.serviceopen.adapter.exam.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/**
 * Adapter-boundary score rules.
 *
 * <p>The Protobuf request carries no Jakarta constraints, so every score rule runs on the mapped
 * carrier; the wire exposes no relation that native constraints cannot express.</p>
 */
@Component("scoreFacadeValidator")
@RequiredArgsConstructor
@Slf4j
public class ScoreFacadeValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public <T> T validateCarrier(T carrier) {
        return validateBean(carrier);
    }
}
