package top.egon.cola.component.tianshu.admin.service.ai.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限定 Duration 的秒数闭区间；null 视为有效，由 @NotNull 负责必填。
 * / Restricts a Duration to an inclusive range of seconds; null is valid and @NotNull handles presence.
 */
@Documented
@Constraint(validatedBy = DurationRangeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDurationRange {

    long minSeconds();

    long maxSeconds();

    String message() default "duration is out of range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
