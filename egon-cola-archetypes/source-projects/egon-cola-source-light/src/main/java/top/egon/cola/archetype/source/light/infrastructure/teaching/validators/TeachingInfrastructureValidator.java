package top.egon.cola.archetype.source.light.infrastructure.teaching.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.ExternalCourse;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Infrastructure response rules for the teaching side. */
@Component("teachingInfrastructureValidator")
@RequiredArgsConstructor
@Slf4j
public class TeachingInfrastructureValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validateExternalCourse(ExternalCourse course, CourseCode expectedCode) {
        if (course == null || !expectedCode.equals(course.code())) {
            throw new TeachingDomainException(
                    "INVALID_EXTERNAL_COURSE", "external course response is invalid");
        }
    }

    public TeachingDomainException invalidCachePayload(Object payload, Throwable cause) {
        return new TeachingDomainException(
                "INVALID_COURSE_CACHE", "course cache payload is invalid", cause);
    }

    public TeachingDomainException persistenceFailure(Throwable cause) {
        return new TeachingDomainException(
                "TEACHING_PERSISTENCE_FAILED", "teaching persistence failed", cause);
    }

    public void requirePublished(boolean published) {
        if (!published) {
            throw new TeachingDomainException(
                    "TEACHING_EVENT_PUBLISH_FAILED", "teaching event publication failed");
        }
    }
}
