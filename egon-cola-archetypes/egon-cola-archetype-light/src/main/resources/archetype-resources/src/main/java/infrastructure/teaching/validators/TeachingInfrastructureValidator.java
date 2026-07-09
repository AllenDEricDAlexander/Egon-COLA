package ${package}.infrastructure.teaching.validators;

import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;
import org.springframework.stereotype.Component;

@Component
public class TeachingInfrastructureValidator {
    public void validateExternalCourse(ExternalCourse course, CourseCode expectedCode) {
        if (course == null || !expectedCode.equals(course.code())) {
            throw new TeachingDomainException(
                    "INVALID_EXTERNAL_COURSE", "external course response is invalid");
        }
    }

    public TeachingDomainException invalidCachePayload(String payload, Throwable cause) {
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
