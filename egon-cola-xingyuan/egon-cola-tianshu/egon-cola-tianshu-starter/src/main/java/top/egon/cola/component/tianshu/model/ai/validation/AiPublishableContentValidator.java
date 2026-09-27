package top.egon.cola.component.tianshu.model.ai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

public class AiPublishableContentValidator
        implements ConstraintValidator<PublishableAiContent, DdcAiContentDTO> {

    @Override
    public boolean isValid(DdcAiContentDTO content, ConstraintValidatorContext context) {
        if (content == null) {
            return false;
        }
        if (content.getPromptText() != null) {
            return !content.getPromptText().isBlank();
        }
        if (content.getNodes() == null) {
            return false;
        }
        return content.getNodes().stream()
                .filter(node -> node != null && node.getNodeType() == DdcAiNodeTypeEnum.FILE)
                .map(DdcAiNodeDTO::getContent)
                .anyMatch(fileContent -> fileContent != null);
    }
}
