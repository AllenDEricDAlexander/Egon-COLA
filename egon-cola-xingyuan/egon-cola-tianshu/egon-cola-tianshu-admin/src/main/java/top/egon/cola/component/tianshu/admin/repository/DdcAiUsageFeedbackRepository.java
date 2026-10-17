package top.egon.cola.component.tianshu.admin.repository;

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
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 使用样本仓储；唯一冲突原样抛出，CAS 失败返回空。 / Usage-sample repository; unique conflicts propagate, CAS misses return empty. */
@Validated
public interface DdcAiUsageFeedbackRepository {

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiUsageFeedbackBO insert(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class) @Valid DdcAiUsageFeedbackBO feedback);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiUsageFeedbackBO> findActiveById(@NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    /** 须在事务内调用；返回已加锁的待评分样本。 / Must run in a transaction; returns locked pending samples. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiUsageFeedbackBO> findClaimableForJudge(
            @NotEmpty(groups = EgonColaModelValidationGroups.Query.class) Collection<@Positive(groups = EgonColaModelValidationGroups.Query.class) Long> resourceIds,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant now,
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 10, groups = EgonColaModelValidationGroups.Query.class) int maxAttempts,
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    /** 按版本 CAS 更新；被并发修改时返回空。 / Version-CAS update; returns empty on a concurrent change. */
    Optional<DdcAiUsageFeedbackBO> update(@NotNull @Valid DdcAiUsageFeedbackBO feedback);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiScoreAggregateBO aggregate(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long versionId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowStart,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowEnd);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiUsageFeedbackBO> findWorst(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long versionId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowStart,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowEnd,
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 200, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiUsageFeedbackBO> page(@NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiUsageFeedbackPageQuery query);

    /** 须在事务内调用；软删除最多 limit 条早于 before 的样本。 / Must run in a transaction; soft-deletes up to limit samples older than before. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    int softDeleteExpired(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant before,
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 5000, groups = EgonColaModelValidationGroups.Query.class) int limit);
}
