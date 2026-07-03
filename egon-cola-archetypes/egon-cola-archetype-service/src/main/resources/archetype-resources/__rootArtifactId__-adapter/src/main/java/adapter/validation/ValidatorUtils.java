#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("validatorUtils")
@RequiredArgsConstructor
public class ValidatorUtils {

    private final Validator validator;

    public <T> void validate(T target, Class<?>... groups) {
        if (target == null) {
            throw new ValidationException("request must not be null");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(target, groups);
        if (!violations.isEmpty()) {
            throw new ValidationException(toMessage(violations));
        }
    }

    private <T> String toMessage(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .sorted(Comparator.comparing((ConstraintViolation<T> violation) -> violation.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
    }
}
