package top.egon.cola.component.tianshu.admin.service.ai.impl;

import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;

import java.util.List;

/** 把低分样本渲染为改写提示段落，受字节预算约束。 / Renders low-score samples as a rewrite-prompt section within a byte budget. */
final class DdcAiOptimizerSamples {

    private DdcAiOptimizerSamples() {
    }

    static String render(List<DdcAiUsageFeedbackBO> samples, int budgetBytes) {
        if (samples.isEmpty()) return "(no scored samples)";
        int perSample = Math.max(256, budgetBytes / samples.size());
        StringBuilder section = new StringBuilder();
        int index = 1;
        for (DdcAiUsageFeedbackBO sample : samples) {
            Short score = sample.getExplicitScore() != null ? sample.getExplicitScore() : sample.getJudgeScore();
            section.append("### Sample ").append(index++)
                    .append(" (score: ").append(score == null ? "n/a" : score)
                    .append(", outcome: ").append(sample.getOutcome() == null ? "UNKNOWN" : sample.getOutcome().getValue())
                    .append(")\nInput: ").append(DdcAiText.truncateUtf8(nullToEmpty(sample.getInputExcerpt()), perSample / 2))
                    .append("\nOutput: ").append(DdcAiText.truncateUtf8(nullToEmpty(sample.getOutputExcerpt()), perSample / 2))
                    .append("\n\n");
        }
        return section.toString().strip();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
