#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.validators.exam;

import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class ExamApplicationValidator {
    public void notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApplicationException(
                    ApplicationErrorCode.VALIDATION_FAILED, field + " must not be blank");
        }
    }
}
