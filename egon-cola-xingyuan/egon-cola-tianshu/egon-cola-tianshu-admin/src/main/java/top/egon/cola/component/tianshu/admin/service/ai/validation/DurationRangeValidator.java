package top.egon.cola.component.tianshu.admin.service.ai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;

/** {@link ValidDurationRange} 的校验实现。 / Validator of {@link ValidDurationRange}. */
public class DurationRangeValidator implements ConstraintValidator<ValidDurationRange, Duration> {

    private long minSeconds;

    private long maxSeconds;

    @Override
    public void initialize(ValidDurationRange annotation) {
        if (annotation.minSeconds() < 0 || annotation.maxSeconds() < annotation.minSeconds()) {
            throw new IllegalArgumentException("Invalid duration range");
        }
        minSeconds = annotation.minSeconds();
        maxSeconds = annotation.maxSeconds();
    }

    @Override
    public boolean isValid(Duration value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value.isNegative() || value.getNano() != 0) return false;
        long seconds = value.toSeconds();
        return seconds >= minSeconds && seconds <= maxSeconds;
    }
}
