package top.egon.cola.archetype.source.lightopen.adapter.teaching.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.ScheduleCourseRequest;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Adapter-boundary teaching rules on top of the native constraints carried by the carriers. */
@Component("teachingRequestValidator")
@RequiredArgsConstructor
@Slf4j
public class TeachingRequestValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validateSchedule(ScheduleCourseRequest request) {
        validateBean(request);
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    public CreateCourseDTO validate(CreateCourseDTO request) {
        return validateBean(request);
    }
}
