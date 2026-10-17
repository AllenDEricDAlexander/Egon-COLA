package top.egon.cola.component.tianshu.admin.model.bo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

import java.util.List;

/** 一次改写所需的基线内容、附加指令与低分样本。 / Baseline content, operator instruction and low-score samples of one rewrite. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiOptimizationContextBO {

    @NotNull
    private DdcAiKindEnum kind;

    @NotNull
    private DdcAiContentDTO baseline;

    @NotNull
    private String instruction;

    @NotNull
    private List<DdcAiUsageFeedbackBO> samples;

    private int maxContextBytes;
}
