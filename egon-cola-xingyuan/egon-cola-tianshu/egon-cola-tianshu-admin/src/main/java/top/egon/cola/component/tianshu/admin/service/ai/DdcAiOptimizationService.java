package top.egon.cola.component.tianshu.admin.service.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationPolicyCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationTriggerCommand;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationPolicyResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationRunResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.time.Instant;
import java.util.List;

/**
 * 优化策略、运行与评估执行。REST 方法校验用户作用域；作业方法在系统主体下运行。
 * / Optimization policies, runs and evaluation. REST methods check user scope; job methods run as a system actor.
 */
@Validated
public interface DdcAiOptimizationService {

    DdcAiOptimizationPolicyResult getPolicy(@NotNull @Valid DdcAiIdQuery query);

    DdcAiOptimizationPolicyResult replacePolicy(@NotNull @Valid DdcAiIdQuery query,
            @NotNull @Valid DdcAiOptimizationPolicyCommand command);

    DdcAiOptimizationRunResult trigger(@NotNull @Valid DdcAiIdQuery query,
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId,
            @NotNull @Valid DdcAiOptimizationTriggerCommand command);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiOptimizationRunResult> pageRuns(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiOptimizationRunPageQuery query);

    DdcAiOptimizationRunResult getRun(@NotNull @Valid DdcAiIdQuery query, @NotNull @Positive Long runId);

    /** 回收租约过期的运行，返回回收数。 / Reclaims runs whose lease expired and returns the count. */
    int reclaimExpiredRuns(@NotNull Instant now);

    /** 评估本租户可评估的策略，返回新建运行 ID。 / Evaluates this tenant's policies and returns created run IDs. */
    List<Long> evaluateTenant(@NotNull Instant now);

    List<Long> pendingRunIds(@Min(1) @Max(500) int limit);

    /** 认领待执行运行；被其他实例抢先时返回 false。 / Claims a pending run; returns false when another instance won. */
    boolean claimRun(@NotNull @Positive Long runId, @NotBlank @Size(max = 128) String owner, @NotNull Instant now);

    void executeRun(@NotNull @Positive Long runId);

    /** 资源删除时同事务软删除策略。 / Soft-deletes the policy in the resource-deletion transaction. */
    void onResourceDeleted(@NotNull @Positive Long resourceId);
}
