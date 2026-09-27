package top.egon.cola.component.tianshu.model.ai.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AiVersionSelectorValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsistentAiVersionSelector {

    String message() default "versionNo requires an explicit resourceId";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
