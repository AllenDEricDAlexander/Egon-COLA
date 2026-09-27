package top.egon.cola.component.tianshu.model.ai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.nio.charset.StandardCharsets;

public class AiNodeValidator implements ConstraintValidator<ValidAiNode, DdcAiNodeDTO> {

    private static final int MAX_PATH_BYTES = 1024;
    private static final int MAX_FILE_BYTES = 262_144;
    private static final int MAX_DEPTH = 16;

    @Override
    public boolean isValid(DdcAiNodeDTO node, ConstraintValidatorContext context) {
        return node == null || hasValidShape(node);
    }

    static boolean hasValidShape(DdcAiNodeDTO node) {
        if (node == null || node.getPath() == null || node.getNodeType() == null) {
            return false;
        }
        String path = node.getPath();
        if (!isValidText(path, true) || path.getBytes(StandardCharsets.UTF_8).length > MAX_PATH_BYTES) {
            return false;
        }
        if (path.isEmpty()) {
            return node.getNodeType() == DdcAiNodeTypeEnum.DIRECTORY && node.getContent() == null;
        }
        if (path.startsWith("/") || path.endsWith("/") || path.indexOf('\\') >= 0) {
            return false;
        }
        String[] segments = path.split("/", -1);
        if (segments.length > MAX_DEPTH) {
            return false;
        }
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        if (node.getNodeType() == DdcAiNodeTypeEnum.DIRECTORY) {
            return node.getContent() == null;
        }
        String content = node.getContent();
        return content != null
                && isValidText(content, false)
                && content.getBytes(StandardCharsets.UTF_8).length <= MAX_FILE_BYTES;
    }

    static boolean isValidText(String value, boolean rejectControlCharacters) {
        if (value == null) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\u0000' || (rejectControlCharacters && Character.isISOControl(current))) {
                return false;
            }
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return false;
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return false;
            }
        }
        return true;
    }
}
