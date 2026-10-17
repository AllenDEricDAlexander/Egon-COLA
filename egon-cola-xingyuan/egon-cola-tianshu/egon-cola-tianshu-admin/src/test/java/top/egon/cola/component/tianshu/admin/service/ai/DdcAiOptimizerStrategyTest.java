package top.egon.cola.component.tianshu.admin.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationContextBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiPromptTemplateLoader;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcPromptOptimizerStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcSkillOptimizerStrategy;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiOptimizerStrategyTest {

    private final DdcAiPromptTemplateLoader templates = new DdcAiPromptTemplateLoader();
    private final DdcPromptOptimizerStrategy prompt = new DdcPromptOptimizerStrategy(templates, new ObjectMapper());
    private final DdcSkillOptimizerStrategy skill = new DdcSkillOptimizerStrategy(templates, new ObjectMapper());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void promptComposeIncludesBaselineInstructionAndWorstSamples() {
        DdcAiModelRequestDTO request = prompt.compose(context(DdcAiKindEnum.PROMPT,
                DdcAiContentDTO.builder().promptText("base prompt").nodes(List.of()).build(), "更简洁"),
                "qwen-plus", 8192, 0.2d);

        assertThat(request.purpose()).isEqualTo("OPTIMIZE");
        assertThat(request.systemPrompt()).isEqualTo(templates.optimizePromptSystem());
        assertThat(request.userPrompt()).contains("## Current Prompt\nbase prompt", "## Operator Instruction\n更简洁",
                "## Low-score Samples", "score: 12", "Input: 用户问题", "Output: 抱歉");
        assertThat(request.maxOutputTokens()).isEqualTo(8192);
    }

    @Test
    void promptParseYieldsValidPromptContent() {
        DdcAiContentDTO content = prompt.parse("```json\n{\"promptText\": \"improved prompt\"}\n```");

        assertThat(content.getPromptText()).isEqualTo("improved prompt");
        assertThat(content.getNodes()).isEmpty();
        assertThat(validator.validate(content, DdcAiValidationGroups.Save.class)).isEmpty();
        assertThatThrownBy(() -> prompt.parse("{\"promptText\": \"  \"}")).hasMessage("INVALID_OUTPUT");
        assertThatThrownBy(() -> prompt.parse("plain text")).hasMessage("INVALID_OUTPUT");
    }

    @Test
    void skillComposeSerializesTheFileTree() {
        DdcAiContentDTO tree = DdcAiContentDTO.builder().nodes(List.of(
                DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build(),
                DdcAiNodeDTO.builder().path("SKILL.md").nodeType(DdcAiNodeTypeEnum.FILE).content("# refund").build()))
                .build();

        DdcAiModelRequestDTO request = skill.compose(context(DdcAiKindEnum.SKILL, tree, ""), "qwen-plus", 8192, 0.2d);

        assertThat(request.userPrompt()).contains("{\"files\":[{\"path\":\"SKILL.md\",\"content\":\"# refund\"}]}",
                "## Operator Instruction\n(none)");
    }

    @Test
    void skillParseBuildsRootAndIntermediateDirectoriesThatPassContentValidation() {
        DdcAiContentDTO content = skill.parse("{\"files\":[{\"path\":\"SKILL.md\",\"content\":\"# s\"},"
                + "{\"path\":\"ref/deep/a.md\",\"content\":\"a\"}]}");

        assertThat(content.getPromptText()).isNull();
        assertThat(content.getNodes()).extracting(DdcAiNodeDTO::getPath)
                .containsExactly("", "SKILL.md", "ref", "ref/deep", "ref/deep/a.md");
        assertThat(validator.validate(content, DdcAiValidationGroups.Save.class)).isEmpty();
    }

    @Test
    void skillParseRejectsTraversalAbsoluteDuplicateAndMissingRoot() {
        for (String text : new String[] {
                "{\"files\":[{\"path\":\"SKILL.md\",\"content\":\"\"},{\"path\":\"../x\",\"content\":\"\"}]}",
                "{\"files\":[{\"path\":\"SKILL.md\",\"content\":\"\"},{\"path\":\"/etc/x\",\"content\":\"\"}]}",
                "{\"files\":[{\"path\":\"SKILL.md\",\"content\":\"\"},{\"path\":\"SKILL.md\",\"content\":\"\"}]}",
                "{\"files\":[{\"path\":\"a.md\",\"content\":\"\"}]}",
                "{\"files\":[{\"path\":\"SKILL.md\",\"content\":1}]}",
                "{\"files\":[]}",
                "not json"}) {
            assertThatThrownBy(() -> skill.parse(text)).as(text).hasMessage("INVALID_OUTPUT");
        }
    }

    private static DdcAiOptimizationContextBO context(DdcAiKindEnum kind, DdcAiContentDTO baseline, String instruction) {
        return DdcAiOptimizationContextBO.builder().kind(kind).baseline(baseline).instruction(instruction)
                .samples(List.of(DdcAiUsageFeedbackBO.builder().judgeScore((short) 12)
                        .outcome(DdcAiOutcomePersistenceEnum.FAILURE).inputExcerpt("用户问题").outputExcerpt("抱歉")
                        .build()))
                .maxContextBytes(16384).build();
    }
}
