package top.egon.cola.component.tianshu.model.ai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiContentValidator implements ConstraintValidator<ValidAiContent, DdcAiContentDTO> {

    private static final int MAX_TREE_NODES = 257;
    private static final long MAX_SKILL_BYTES = 1_048_576L;

    @Override
    public boolean isValid(DdcAiContentDTO content, ConstraintValidatorContext context) {
        if (content == null || content.getNodes() == null) {
            return false;
        }
        String promptText = content.getPromptText();
        List<DdcAiNodeDTO> nodes = content.getNodes();
        if (promptText != null) {
            return nodes.isEmpty() && AiNodeValidator.isValidText(promptText, false);
        }
        return isValidSkillTree(nodes);
    }

    private boolean isValidSkillTree(List<DdcAiNodeDTO> nodes) {
        if (nodes.isEmpty() || nodes.size() > MAX_TREE_NODES) {
            return false;
        }
        Map<String, DdcAiNodeDTO> nodesByPath = new HashMap<>();
        long totalBytes = 0;
        for (DdcAiNodeDTO node : nodes) {
            if (!AiNodeValidator.hasValidShape(node) || nodesByPath.putIfAbsent(node.getPath(), node) != null) {
                return false;
            }
            if (node.getNodeType() == DdcAiNodeTypeEnum.FILE) {
                totalBytes += node.getContent().getBytes(StandardCharsets.UTF_8).length;
                if (totalBytes > MAX_SKILL_BYTES) {
                    return false;
                }
            }
        }
        DdcAiNodeDTO root = nodesByPath.get("");
        if (root == null || root.getNodeType() != DdcAiNodeTypeEnum.DIRECTORY) {
            return false;
        }
        for (DdcAiNodeDTO node : nodes) {
            if (node.getPath().isEmpty()) {
                continue;
            }
            int parentSeparator = node.getPath().lastIndexOf('/');
            String parentPath = parentSeparator < 0 ? "" : node.getPath().substring(0, parentSeparator);
            DdcAiNodeDTO parent = nodesByPath.get(parentPath);
            if (parent == null || parent.getNodeType() != DdcAiNodeTypeEnum.DIRECTORY) {
                return false;
            }
        }
        return true;
    }
}
