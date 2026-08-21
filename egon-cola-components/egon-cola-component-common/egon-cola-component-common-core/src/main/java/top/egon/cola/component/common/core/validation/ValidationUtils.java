package top.egon.cola.component.common.core.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Stateless facade for manual Jakarta Bean Validation at an object boundary.
 */
public final class ValidationUtils {

    private static final Comparator<ConstraintViolation<?>> VIOLATION_ORDER = Comparator
            .comparing((ConstraintViolation<?> violation) -> violation.getPropertyPath().toString())
            .thenComparing(violation -> violation.getConstraintDescriptor()
                    .getAnnotation().annotationType().getName())
            .thenComparing(ConstraintViolation::getMessage);

    private final Validator validator;

    public ValidationUtils(Validator validator) {
        this.validator = requireArgument(validator, "validator");
    }

    public <T> T validate(T target, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = violations(target, groups);
        throwIfInvalid(violations);
        return target;
    }

    public <T> Set<ConstraintViolation<T>> violations(T target, Class<?>... groups) {
        T checkedTarget = requireArgument(target, "target");
        Class<?>[] normalizedGroups = normalizeGroups(groups);
        return ordered(validator.validate(checkedTarget, normalizedGroups));
    }

    public <T> boolean isValid(T target, Class<?>... groups) {
        return violations(target, groups).isEmpty();
    }

    public <T> T validateProperty(T target, String propertyName, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = propertyViolations(target, propertyName, groups);
        throwIfInvalid(violations);
        return target;
    }

    public <T> Set<ConstraintViolation<T>> propertyViolations(
            T target, String propertyName, Class<?>... groups) {
        T checkedTarget = requireArgument(target, "target");
        String checkedProperty = requirePropertyName(propertyName);
        Class<?>[] normalizedGroups = normalizeGroups(groups);
        return ordered(validator.validateProperty(checkedTarget, checkedProperty, normalizedGroups));
    }

    public <T> void validateValue(
            Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = valueViolations(beanType, propertyName, value, groups);
        throwIfInvalid(violations);
    }

    public <T> Set<ConstraintViolation<T>> valueViolations(
            Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        Class<T> checkedBeanType = requireArgument(beanType, "beanType");
        String checkedProperty = requirePropertyName(propertyName);
        Class<?>[] normalizedGroups = normalizeGroups(groups);
        return ordered(validator.validateValue(checkedBeanType, checkedProperty, value, normalizedGroups));
    }

    private static Class<?>[] normalizeGroups(Class<?>[] groups) {
        if (groups == null) {
            throw new IllegalArgumentException("groups must not be null");
        }
        if (groups.length == 0) {
            return new Class<?>[]{Default.class};
        }
        Class<?>[] copy = groups.clone();
        for (Class<?> group : copy) {
            requireArgument(group, "group");
        }
        return copy;
    }

    private static String requirePropertyName(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            throw new IllegalArgumentException("propertyName must not be blank");
        }
        return propertyName;
    }

    private static <T> Set<ConstraintViolation<T>> ordered(Set<ConstraintViolation<T>> violations) {
        Objects.requireNonNull(violations, "validator returned null violations");
        LinkedHashSet<ConstraintViolation<T>> ordered = violations.stream()
                .sorted(VIOLATION_ORDER)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ordered);
    }

    private static void throwIfInvalid(Set<? extends ConstraintViolation<?>> violations) {
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private static <T> T requireArgument(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
