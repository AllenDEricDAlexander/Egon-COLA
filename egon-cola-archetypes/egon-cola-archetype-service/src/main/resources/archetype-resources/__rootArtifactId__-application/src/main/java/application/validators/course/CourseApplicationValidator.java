#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.validators.course;

import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class CourseApplicationValidator {
    public void require(boolean condition, String message) {
        if (!condition) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, message);
        }
    }
}
