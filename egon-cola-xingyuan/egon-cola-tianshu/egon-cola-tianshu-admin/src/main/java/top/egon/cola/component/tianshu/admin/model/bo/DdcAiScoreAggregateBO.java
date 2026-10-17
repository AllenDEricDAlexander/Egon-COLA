package top.egon.cola.component.tianshu.admin.model.bo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;

/** 一个版本在评估窗口内的有效分聚合。 / Effective-score aggregate of one version within an evaluation window. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiScoreAggregateBO {

    @NotNull
    @Min(0)
    private Integer sampleCount;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal averageScore;

    @NotNull
    @Min(0)
    private Integer pendingJudgeCount;

    @NotNull
    private Instant windowStart;

    @NotNull
    private Instant windowEnd;
}
