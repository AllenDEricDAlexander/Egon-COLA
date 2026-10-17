package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 优化运行仓储；运行不可删除。 / Optimization-run repository; runs are never deleted. */
@Validated
public interface DdcAiOptimizationRunRepository {

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiOptimizationRunBO insert(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class) @Valid DdcAiOptimizationRunBO run);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiOptimizationRunBO> findActiveById(@NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiOptimizationRunBO> findByRequestId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", groups = EgonColaModelValidationGroups.Query.class) String requestId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long nextRunNo(@NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiOptimizationRunBO> findActiveRunForResource(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiOptimizationRunBO> findLatestFinished(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<Long> findPendingIds(@Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationRunBO> findExpiredRunning(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant now,
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    /** 按版本 CAS 更新；被并发修改时返回空。 / Version-CAS update; returns empty on a concurrent change. */
    Optional<DdcAiOptimizationRunBO> update(@NotNull @Valid DdcAiOptimizationRunBO run);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiOptimizationRunBO> page(@NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiOptimizationRunPageQuery query);
}
