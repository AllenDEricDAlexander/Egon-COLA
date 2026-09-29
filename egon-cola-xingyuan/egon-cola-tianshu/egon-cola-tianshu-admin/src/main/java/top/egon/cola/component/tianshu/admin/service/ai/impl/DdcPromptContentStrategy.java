package top.egon.cola.component.tianshu.admin.service.ai.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service("ddcPromptContentStrategy")
@RequiredArgsConstructor
@Validated
public class DdcPromptContentStrategy implements DdcAiContentStrategy {

    private static final byte[] DOMAIN = "tianshu-ai-content-v1".getBytes(StandardCharsets.UTF_8);

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public DdcAiKindEnum kind() {
        return DdcAiKindEnum.PROMPT;
    }

    @Override
    public PreparedContent prepare(DdcAiContentDTO content) {
        validationUtils.validate(content, DdcAiValidationGroups.Save.class);
        String promptText = Objects.requireNonNull(content.getPromptText(), "AI_PROMPT_TEXT_REQUIRED");
        if (content.getNodes() == null || !content.getNodes().isEmpty()) {
            throw new IllegalArgumentException("AI_PROMPT_NODES_MUST_BE_EMPTY");
        }
        byte[] body = promptText.getBytes(StandardCharsets.UTF_8);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(DOMAIN.length + body.length + Long.BYTES + 2);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.write(DOMAIN);
                output.writeByte(0);
                output.writeByte(1);
                output.writeLong(body.length);
                output.write(body);
            }
            return new PreparedContent(promptText, List.of(), DdcAiContentStrategy.sha256Hex(bytes.toByteArray()),
                    Map.of(), body.length, 0);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to encode Prompt content hash", failure);
        }
    }
}
