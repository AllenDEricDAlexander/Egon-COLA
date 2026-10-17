package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 优化策略仓储。 / Optimization-policy repository. */
@Validated
public interface DdcAiOptimizationPolicyRepository {

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiOptimizationPolicyBO insert(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class) @Valid DdcAiOptimizationPolicyBO policy);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiOptimizationPolicyBO> findActiveByResourceId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    /** 须在事务内调用。 / Must run in a transaction. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiOptimizationPolicyBO> findActiveByResourceIdForUpdate(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    /** 须在事务内调用；跳过被其他实例锁定的行。 / Must run in a transaction; skips rows locked by other instances. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationPolicyBO> findEvaluableForUpdate(
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    Set<Long> findJudgeEnabledResourceIds();

    /** 按版本 CAS 更新；被并发修改时返回空。 / Version-CAS update; returns empty on a concurrent change. */
    Optional<DdcAiOptimizationPolicyBO> update(@NotNull @Valid DdcAiOptimizationPolicyBO policy);

    boolean delete(@NotNull @Valid DdcAiOptimizationPolicyBO policy);
}
