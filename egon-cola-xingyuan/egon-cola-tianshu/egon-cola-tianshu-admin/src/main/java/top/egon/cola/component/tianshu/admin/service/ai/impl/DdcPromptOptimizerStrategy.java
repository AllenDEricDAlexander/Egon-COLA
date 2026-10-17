package top.egon.cola.component.tianshu.admin.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationContextBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

import java.util.List;

/** Prompt 改写策略：输出 {"promptText": "..."}。 / Prompt rewrite strategy producing {"promptText": "..."}. */
@Slf4j
@Component("ddcPromptOptimizerStrategy")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DdcPromptOptimizerStrategy implements DdcAiOptimizerStrategy {

    @Qualifier("ddcAiPromptTemplateLoader")
    private final DdcAiPromptTemplateLoader templates;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    public DdcAiKindEnum kind() {
        return DdcAiKindEnum.PROMPT;
    }

    @Override
    public DdcAiModelRequestDTO compose(DdcAiOptimizationContextBO context, String modelAlias, int maxOutputTokens,
            double temperature) {
        String baseline = context.getBaseline().getPromptText() == null ? "" : context.getBaseline().getPromptText();
        String user = "## Current Prompt\n" + DdcAiText.truncateUtf8(baseline, context.getMaxContextBytes() / 2)
                + "\n\n## Operator Instruction\n" + (context.getInstruction().isBlank() ? "(none)" : context.getInstruction())
                + "\n\n## Low-score Samples\n"
                + DdcAiOptimizerSamples.render(context.getSamples(), context.getMaxContextBytes() / 2);
        return new DdcAiModelRequestDTO(modelAlias, templates.optimizePromptSystem(), user, maxOutputTokens, temperature,
                "OPTIMIZE");
    }

    @Override
    public DdcAiContentDTO parse(String modelText) {
        try {
            JsonNode node = objectMapper.readTree(DdcAiText.stripCodeFence(modelText));
            JsonNode prompt = node == null ? null : node.get("promptText");
            if (prompt == null || !prompt.isTextual() || prompt.asText().isBlank()) throw invalid();
            return DdcAiContentDTO.builder().promptText(prompt.asText()).nodes(List.of()).build();
        } catch (JsonProcessingException malformed) {
            throw invalid();
        }
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("INVALID_OUTPUT");
    }
}
