package top.egon.cola.component.yuheng.admin.llm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 中文说明：{@code ValidLlmModel} 是类级自定义校验约束注解，负责模型别名命令内 embedding 载体与声明 kind 一致性的校验边界。
 * English summary: {@code ValidLlmModel} is a class-level custom constraint annotation that owns the consistency boundary between the embedding carriers and the declared kind of a model alias command.
 *
 * 用法 / Usage: 标注在对应 CommandDTO 上并与所选 Create/Update 分组一起触发；跨字段规则由 {@code LlmModelValidator} 执行，必填与范围等单字段规则仍由原生约束表达，违规按属性节点上报以保留原 422 字段路径。/ Attach it to the matching command carrier and activate it together with the selected Create/Update groups; the cross-field rule runs in {@code LlmModelValidator}, presence and range rules stay native constraints, and violations are reported on property nodes so the standard 422 body keeps the original field paths.
 *
 * @see LlmModelValidator
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LlmModelValidator.class)
public @interface ValidLlmModel {

    /**
     * Message template reported on the offending carrier property so the
     * standard 422 body keeps the original field path.
     *
     * @return the default message template
     */
    String message() default
            "dimensions and embeddingSpaceId are required for EMBEDDING"
                    + " and must be null for CHAT";

    /**
     * Validation groups this constraint belongs to.
     *
     * @return the configured groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload types carried by the produced constraint violations.
     *
     * @return the configured payload
     */
    Class<? extends Payload>[] payload() default {};
}
