package top.egon.cola.component.tianshu.admin.service.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiScoreAggregateBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportBatchCommand;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiJudgeVerdictDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageFeedbackResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageReportReceiptResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 使用反馈的接收、评分状态推进、聚合与保留期清理。
 * / Intake of usage feedback, judge-state progression, aggregation and retention cleanup.
 */
@Validated
public interface DdcAiFeedbackService {

    /** 逐条受理批量上报；每条独立事务。 / Accepts a batch item by item, each in its own transaction. */
    DdcAiUsageReportReceiptResult accept(@NotNull @Valid DdcAiUsageReportBatchCommand command);

    /** 认领待评分样本并置为 JUDGING。 / Claims pending samples and moves them to JUDGING. */
    List<DdcAiUsageFeedbackBO> claimForJudge(
            @NotEmpty Set<@Positive Long> resourceIds, @Min(1) @Max(500) int limit);

    /** 写回评分；被接管时静默放弃。 / Writes the judgement back; silently gives up when another worker took over. */
    void completeJudge(@NotNull @Positive Long id, @NotNull @Min(0) Long version,
            @NotNull @Valid DdcAiJudgeVerdictDTO verdict);

    /** 记录评分失败；达到上限后置 FAILED。 / Records a judge failure; moves to FAILED at the attempt limit. */
    void failJudge(@NotNull @Positive Long id, @NotNull @Min(0) Long version, @NotNull DdcAiRunErrorCodeEnum code);

    DdcAiScoreAggregateBO aggregate(@NotNull @Positive Long resourceId, @NotNull @Positive Long versionId,
            @NotNull Instant windowStart, @NotNull Instant windowEnd);

    List<DdcAiUsageFeedbackBO> worstSamples(@NotNull @Positive Long resourceId, @NotNull @Positive Long versionId,
            @NotNull Instant windowStart, @NotNull Instant windowEnd, @Min(1) @Max(200) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiUsageFeedbackResult> pageFeedback(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiUsageFeedbackPageQuery query);

    /** 软删除超过保留期的样本，返回删除条数。 / Soft-deletes samples past retention and returns the count. */
    int purgeExpired(@Min(1) @Max(5000) int limit);
}
