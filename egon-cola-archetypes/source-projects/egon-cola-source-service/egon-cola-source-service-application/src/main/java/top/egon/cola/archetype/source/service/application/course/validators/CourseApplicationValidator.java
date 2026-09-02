package top.egon.cola.archetype.source.service.application.course.validators;

import top.egon.cola.archetype.source.service.application.exceptions.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.application.exceptions.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class CourseApplicationValidator {
    public void require(boolean condition, String message) {
        if (!condition) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, message);
        }
    }
}
