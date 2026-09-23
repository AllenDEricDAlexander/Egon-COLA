package top.egon.cola.component.yuheng.admin.llm.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmModelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;

/**
 * 中文说明：{@code LlmModelValidator} 是 {@code ValidLlmModel} 的 ConstraintValidator 实现，负责在输入侧校验模型别名命令的 embedding 载体与 kind 是否一致。
 * English summary: {@code LlmModelValidator} is the ConstraintValidator of {@code ValidLlmModel} and checks on the input side that the embedding carriers of a model alias command match its declared kind.
 *
 * 用法 / Usage: 由容器按约束注解调用，不查库、不发外部调用、不决定权限；EMBEDDING 的每条 route 必须落在 deployment=LOCAL 的 channel 上，这需要读取已存 channel，因此由所属 Service 在事务内复查，而数值范围与必填等单字段规则由原生约束表达。/ Invoked by the container through the constraint annotation without database, network or permission access; whether every route of an EMBEDDING model points at a LOCAL deployment channel needs the stored channels and is therefore repeated by the owning Service inside its transaction, while single-field range and presence rules stay native constraints.
 */
@Slf4j
@Component("llmModelValidator")
@RequiredArgsConstructor
public class LlmModelValidator
        implements ConstraintValidator<ValidLlmModel, LlmModelCommandDTO> {

    private static final String DIMENSIONS_PROPERTY = "dimensions";

    private static final String EMBEDDING_SPACE_PROPERTY = "embeddingSpaceId";

    /**
     * Checks the embedding carriers of one command against its model kind.
     *
     * @param command model alias command, may be absent
     * @param context constraint context used for property-node violations
     * @return {@code true} when the carriers are consistent or nothing is set
     */
    @Override
    public boolean isValid(
            LlmModelCommandDTO command,
            ConstraintValidatorContext context) {
        if (command == null || command.getKind() == null) {
            // Absent input belongs to the carrier's own presence constraints.
            return true;
        }
        boolean embedding = LlmModelKindEnum.EMBEDDING == command.getKind();
        Integer dimensions = command.getDimensions();
        String embeddingSpaceId = command.getEmbeddingSpaceId();
        boolean dimensionsConsistent = embedding == (dimensions != null);
        boolean spaceConsistent = embedding
                ? embeddingSpaceId != null && !embeddingSpaceId.isBlank()
                : embeddingSpaceId == null;
        if (dimensionsConsistent && spaceConsistent) {
            return true;
        }
        log.debug(
                "Rejected inconsistent LLM model carriers key={} embedding={}",
                command.getKey(),
                embedding
        );
        context.disableDefaultConstraintViolation();
        if (!dimensionsConsistent) {
            rejectProperty(context, DIMENSIONS_PROPERTY);
        }
        if (!spaceConsistent) {
            rejectProperty(context, EMBEDDING_SPACE_PROPERTY);
        }
        return false;
    }

    private static void rejectProperty(
            ConstraintValidatorContext context,
            String property) {
        context.buildConstraintViolationWithTemplate(
                        context.getDefaultConstraintMessageTemplate()
                )
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
