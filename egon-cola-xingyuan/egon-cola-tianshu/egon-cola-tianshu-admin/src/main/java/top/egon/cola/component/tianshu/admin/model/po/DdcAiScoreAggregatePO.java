package top.egon.cola.component.tianshu.admin.model.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 使用样本聚合查询的只读投影行。 / Read-only projection row of the usage-sample aggregate query. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiScoreAggregatePO {

    /** 有效分非空的样本数。 / Number of samples with a non-null effective score. */
    private Long sampleCount;

    /** 有效分之和；平均分由仓储计算以避免分片聚合改写。 / Sum of effective scores; the repository computes the average. */
    private Long scoreSum;

    private Long pendingJudgeCount;
}
