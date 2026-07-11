#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.validators;

import ${package}.domain.common.EvaluationPortException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class EvaluationPersistenceValidator {
    public EvaluationPortException translate(String operation, DataIntegrityViolationException failure) {
        String constraint = constraintName(failure);
        String message = constraint == null
                ? "persistence operation failed"
                : "persistence constraint violated: " + constraint;
        return new EvaluationPortException(operation, message, failure);
    }

    private String constraintName(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                for (String constraint : new String[] {
                        "uk_course_code", "uk_exam_paper_exam", "uk_score_exam_student"}) {
                    if (lower.contains(constraint)) return constraint;
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
