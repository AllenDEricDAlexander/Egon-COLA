package top.egon.cola.component.tianshu.admin.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiJudgeContextBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiPromptTemplateLoader;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcDefaultAiJudgeStrategy;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiJudgeStrategyTest {

    private final DdcAiPromptTemplateLoader templates = new DdcAiPromptTemplateLoader();
    private final DdcDefaultAiJudgeStrategy strategy = new DdcDefaultAiJudgeStrategy(templates, new ObjectMapper());

    @Test
    void composeIncludesTruncatedVersionAndSampleExcerpts() {
        DdcAiModelRequestDTO request = strategy.compose(context("x".repeat(40_000), "用户问题", "模型输出"), 16_384);

        assertThat(request.purpose()).isEqualTo("JUDGE");
        assertThat(request.modelAlias()).isEqualTo("qwen-turbo");
        assertThat(request.temperature()).isZero();
        assertThat(request.systemPrompt()).isEqualTo(templates.judgeSystem()).contains("\"score\"");
        assertThat(request.userPrompt()).startsWith("## Prompt\n").contains("## Input\n用户问题", "## Output\n模型输出",
                "## Outcome\nFAILURE");
        assertThat(request.userPrompt().getBytes(StandardCharsets.UTF_8).length).isLessThan(16_384 + 64);
    }

    @Test
    void skillContextUsesTheSkillHeading() {
        DdcAiJudgeContextBO skill = context("# skill", null, null).setKind(DdcAiKindEnum.SKILL);

        assertThat(strategy.compose(skill, 4096).userPrompt()).startsWith("## Skill\n# skill").contains("## Input\n\n");
    }

    @Test
    void parseAcceptsStrictJsonAndCodeFences() {
        assertThat(strategy.parse("{\"score\":66,\"rationale\":\"ok\"}").score()).isEqualTo(66);
        assertThat(strategy.parse("```json\n{\"score\": 12, \"rationale\": \"weak\"}\n```").rationale()).isEqualTo("weak");
        assertThat(strategy.parse("{\"score\":0}").rationale()).isEmpty();
    }

    @Test
    void parseRejectsNonJsonOutOfRangeAndNonIntegralScores() {
        for (String text : new String[] {"not json", "{\"score\":101}", "{\"score\":-1}", "{\"score\":7.5}",
                "{\"rationale\":\"missing\"}", "[]"}) {
            assertThatThrownBy(() -> strategy.parse(text)).as(text).hasMessage("INVALID_OUTPUT");
        }
    }

    private static DdcAiJudgeContextBO context(String versionText, String input, String output) {
        return DdcAiJudgeContextBO.builder().kind(DdcAiKindEnum.PROMPT).judgeModelAlias("qwen-turbo")
                .versionText(versionText).inputExcerpt(input).outputExcerpt(output)
                .outcome(DdcAiOutcomePersistenceEnum.FAILURE).build();
    }
}
