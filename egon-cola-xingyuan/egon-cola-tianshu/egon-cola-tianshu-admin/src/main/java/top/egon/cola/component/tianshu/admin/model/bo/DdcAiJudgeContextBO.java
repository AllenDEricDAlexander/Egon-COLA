package top.egon.cola.component.tianshu.admin.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

/** 一次评分所需的版本内容与样本摘录。 / Version content and sample excerpts needed for one judgement. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiJudgeContextBO {

    @NotNull
    private DdcAiKindEnum kind;

    @NotBlank
    private String judgeModelAlias;

    @NotNull
    private String versionText;

    private String inputExcerpt;

    private String outputExcerpt;

    @NotNull
    private DdcAiOutcomePersistenceEnum outcome;
}
