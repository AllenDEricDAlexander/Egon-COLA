package top.egon.cola.archetype.source.serviceopen.application.course.validators;

import top.egon.cola.archetype.source.serviceopen.application.exceptions.ApplicationErrorCode;
import top.egon.cola.archetype.source.serviceopen.application.exceptions.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class CourseApplicationValidator {
    public void require(boolean condition, String message) {
        if (!condition) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, message);
        }
    }
}
