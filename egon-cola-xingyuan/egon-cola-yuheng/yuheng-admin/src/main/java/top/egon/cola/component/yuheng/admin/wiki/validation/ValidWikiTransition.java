package top.egon.cola.component.yuheng.admin.wiki.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 中文说明：{@code ValidWikiTransition} 是类级自定义校验约束注解，负责 Wiki 生命周期命令内事件与两个 CAS version 必须同时给出的校验边界。
 * English summary: {@code ValidWikiTransition} is a class-level custom constraint annotation that owns the boundary requiring the event and both compare-and-set versions of a Wiki lifecycle command together.
 *
 * 用法 / Usage: 标注在对应 CommandDTO 上并按 Execute 分组触发；跨字段规则由 {@code WikiTransitionValidator} 执行，格式与取值范围等单字段规则仍由原生约束表达；本命令不携带 publicationStatus、reviewStatus 或 reviewer 字段，且管理面对新 AI 命令启用了未知属性失败的严格 JSON 绑定，调用方因此无法改写审查状态。/ Attach it to the matching command carrier and activate it with the Execute group; the cross-field rule runs in {@code WikiTransitionValidator} while format and range rules stay native constraints; the carrier has no publicationStatus, reviewStatus or reviewer field and the admin interface binds the new AI carriers with unknown properties rejected, so a caller cannot smuggle review state or a reviewer override.
 *
 * @see WikiTransitionValidator
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WikiTransitionValidator.class)
public @interface ValidWikiTransition {

    /**
     * Message template reported on the offending carrier property so the
     * standard 422 body keeps the original field path.
     *
     * @return the default message template
     */
    String message() default
            "event, expectedPageRevision and expectedPublicationVersion"
                    + " are required";

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
