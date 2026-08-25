#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.validators;

import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class ExamApplicationValidator {
    public void positive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new ApplicationException(
                    ApplicationErrorCode.VALIDATION_FAILED, field + " must be positive");
        }
    }
}
