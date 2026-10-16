package top.egon.cola.archetype.source.lightopen.adapter.teaching.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Adapter-boundary teaching rules on top of the native constraints carried by the carriers. */
@Component("teachingRequestValidator")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class TeachingRequestValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validateSchedule(ScheduleCourseDTO request) {
        validateBean(request);
        if (!request.startsAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                        .isBefore(request.endsAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS))) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    public CreateCourseDTO validate(CreateCourseDTO request) {
        return validateBean(request);
    }
}
