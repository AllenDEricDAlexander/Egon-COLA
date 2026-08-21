package top.egon.cola.component.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class ValidationUtilsTest {

    private static final Class<?> GROUP = Group.class;

    @Test
    void validateReturnsOriginalTargetAndUsesDefaultGroupWhenNoGroupsAreProvided() {
        Fixture target = new Fixture();
        RecordingValidator validator = new RecordingValidator(Set.of());
        ValidationUtils utils = new ValidationUtils(validator);

        assertSame(target, utils.validate(target));
        assertEquals(List.of("jakarta.validation.groups.Default"), validator.lastGroups());
    }

    @Test
    void violationsAreSortedAndUnmodifiable() {
        Fixture target = new Fixture();
        Set<ConstraintViolation<Fixture>> violations = new LinkedHashSet<>(List.of(
                violation("zeta", "ZConstraint", "z-message"),
                violation("alpha", "AConstraint", "a-message")));
        ValidationUtils utils = new ValidationUtils(new RecordingValidator(violations));

        Set<ConstraintViolation<Fixture>> result = utils.violations(target, GROUP);

        assertEquals(List.of("alpha", "zeta"), result.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList());
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void validateThrowsStandardConstraintViolationExceptionWithStableViolations() {
        Fixture target = new Fixture();
        ValidationUtils utils = new ValidationUtils(new RecordingValidator(Set.of(
                violation("title", "NotBlank", "must not be blank"))));

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class, () -> utils.validate(target, GROUP));

        assertEquals("title", exception.getConstraintViolations().iterator().next()
                .getPropertyPath().toString());
    }

    @Test
    void propertyAndValueFamiliesDelegateWithExplicitGroups() {
        Fixture target = new Fixture();
        RecordingValidator validator = new RecordingValidator(Set.of());
        ValidationUtils utils = new ValidationUtils(validator);

        assertSame(target, utils.validateProperty(target, "title", GROUP));
        assertEquals(Set.of(), utils.propertyViolations(target, "title", GROUP));
        utils.validateValue(Fixture.class, "title", "value", GROUP);
        assertEquals(Set.of(), utils.valueViolations(Fixture.class, "title", "value", GROUP));
        assertEquals(List.of(Group.class.getName()), validator.lastGroups());
    }

    @Test
    void isValidReflectsTheSameViolationSetWithoutMutatingTheTarget() {
        Fixture validTarget = new Fixture();
        Fixture invalidTarget = new Fixture();
        ValidationUtils validUtils = new ValidationUtils(new RecordingValidator(Set.of()));
        ValidationUtils invalidUtils = new ValidationUtils(new RecordingValidator(Set.of(
                violation("title", "NotBlank", "must not be blank"))));

        assertTrue(validUtils.isValid(validTarget, GROUP));
        assertFalse(invalidUtils.isValid(invalidTarget, GROUP));
        assertEquals(null, validTarget.title);
        assertEquals(null, invalidTarget.title);
    }

    @Test
    void invalidArgumentsFailWithIllegalArgumentException() {
        ValidationUtils utils = new ValidationUtils(new RecordingValidator(Set.of()));

        assertThrows(IllegalArgumentException.class, () -> utils.validate(null));
        assertThrows(IllegalArgumentException.class, () -> utils.validate(new Fixture(), (Class<?>[]) null));
        assertThrows(IllegalArgumentException.class, () -> utils.validateProperty(new Fixture(), " "));
        assertThrows(IllegalArgumentException.class, () -> utils.validateValue(null, "title", "value"));
        assertThrows(IllegalArgumentException.class, () -> new ValidationUtils(null));
    }

    private static ConstraintViolation<Fixture> violation(String property, String annotation, String message) {
        Path path = proxy(Path.class, (ignoredProxy, method, ignoredArgs) ->
                "toString".equals(method.getName()) ? property : null);
        Class<? extends Annotation> annotationType = annotationType(annotation);
        Annotation constraintAnnotation = proxy(Annotation.class,
                (ignoredProxy, method, ignoredArgs) -> "annotationType".equals(method.getName())
                        ? annotationType : defaultValue(method.getReturnType()));
        ConstraintDescriptor<Annotation> descriptor = proxy(ConstraintDescriptor.class,
                (ignoredProxy, method, ignoredArgs) -> "getAnnotation".equals(method.getName())
                        ? constraintAnnotation : defaultValue(method.getReturnType()));
        return proxy(ConstraintViolation.class, (ignoredProxy, method, ignoredArgs) -> switch (method.getName()) {
            case "getPropertyPath" -> path;
            case "getConstraintDescriptor" -> descriptor;
            case "getMessage", "getMessageTemplate" -> message;
            case "getInvalidValue" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> annotationType(String name) {
        return (Class<? extends Annotation>) switch (name) {
            case "AConstraint" -> AConstraint.class;
            case "ZConstraint" -> ZConstraint.class;
            default -> NotBlankTest.class;
        };
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    private interface Group {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface AConstraint {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface ZConstraint {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface NotBlankTest {
    }

    private static final class Fixture {
        private String title;
    }

    private static final class RecordingValidator implements Validator {

        private final Set<? extends ConstraintViolation<?>> violations;
        private List<String> lastGroups = List.of();

        private RecordingValidator(Set<? extends ConstraintViolation<?>> violations) {
            this.violations = violations;
        }

        private List<String> lastGroups() {
            return lastGroups;
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
            recordGroups(groups);
            return castViolations();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateProperty(
                T object, String propertyName, Class<?>... groups) {
            recordGroups(groups);
            return castViolations();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateValue(
                Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
            recordGroups(groups);
            return castViolations();
        }

        @Override
        public ExecutableValidator forExecutables() {
            return null;
        }

        @Override
        public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            throw new UnsupportedOperationException(type.getName());
        }

        private void recordGroups(Class<?>[] groups) {
            lastGroups = List.of(groups).stream().map(Class::getName).toList();
        }

        @SuppressWarnings("unchecked")
        private <T> Set<ConstraintViolation<T>> castViolations() {
            return (Set<ConstraintViolation<T>>) (Set<?>) violations;
        }
    }
}
