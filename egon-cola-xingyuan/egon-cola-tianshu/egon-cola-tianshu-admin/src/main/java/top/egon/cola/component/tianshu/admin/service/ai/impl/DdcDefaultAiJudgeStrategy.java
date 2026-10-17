package top.egon.cola.component.tianshu.admin.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiJudgeContextBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiJudgeVerdictDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

/**
 * 默认评分策略：内容占一半上下文预算，输入与输出各占四分之一。
 * / Default judge strategy: content gets half the context budget, input and output a quarter each.
 */
@Slf4j
@Component("ddcDefaultAiJudgeStrategy")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DdcDefaultAiJudgeStrategy implements DdcAiJudgeStrategy {

    static final int MAX_JUDGE_OUTPUT_TOKENS = 512;

    @Qualifier("ddcAiPromptTemplateLoader")
    private final DdcAiPromptTemplateLoader templates;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    public DdcAiModelRequestDTO compose(DdcAiJudgeContextBO context, int maxContextBytes) {
        String heading = context.getKind() == DdcAiKindEnum.PROMPT ? "## Prompt" : "## Skill";
        String user = heading + "\n" + DdcAiText.truncateUtf8(context.getVersionText(), maxContextBytes / 2)
                + "\n\n## Input\n" + DdcAiText.truncateUtf8(nullToEmpty(context.getInputExcerpt()), maxContextBytes / 4)
                + "\n\n## Output\n" + DdcAiText.truncateUtf8(nullToEmpty(context.getOutputExcerpt()), maxContextBytes / 4)
                + "\n\n## Outcome\n" + context.getOutcome().getValue();
        return new DdcAiModelRequestDTO(context.getJudgeModelAlias(), templates.judgeSystem(), user,
                MAX_JUDGE_OUTPUT_TOKENS, 0.0d, "JUDGE");
    }

    @Override
    public DdcAiJudgeVerdictDTO parse(String modelText) {
        try {
            JsonNode node = objectMapper.readTree(DdcAiText.stripCodeFence(modelText));
            JsonNode score = node == null ? null : node.get("score");
            if (score == null || !score.isIntegralNumber() || score.intValue() < 0 || score.intValue() > 100) {
                throw invalid();
            }
            JsonNode rationale = node.get("rationale");
            return new DdcAiJudgeVerdictDTO(score.intValue(), rationale == null ? "" : rationale.asText(""));
        } catch (JsonProcessingException malformed) {
            throw invalid();
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("INVALID_OUTPUT");
    }
}
