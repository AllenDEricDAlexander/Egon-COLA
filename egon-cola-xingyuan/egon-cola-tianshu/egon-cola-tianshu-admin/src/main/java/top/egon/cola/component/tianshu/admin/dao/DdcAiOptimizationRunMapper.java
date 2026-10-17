package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationRunPO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 优化运行的租户内访问；运行记录不可删除。 / Tenant-scoped optimization-run access; runs are never deleted. */
@Mapper
@Validated
public interface DdcAiOptimizationRunMapper extends EgonColaMapper<DdcAiOptimizationRunPO> {

    @Override
    default DdcAiOptimizationRunPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Override
    default List<DdcAiOptimizationRunPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    @Override
    default int deleteVersionedById(DdcAiOptimizationRunPO run) {
        throw new UnsupportedOperationException("AI_OPTIMIZATION_RUN_IMMUTABLE");
    }

    @Override
    default int restoreVersionedById(DdcAiOptimizationRunPO run) {
        throw new UnsupportedOperationException("AI_OPTIMIZATION_RUN_RESTORE_DISABLED");
    }

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationRunPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationRunPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Collection<@Positive(groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationRunPO selectByRequestId(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("requestId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(min = 16, max = 64, groups = EgonColaModelValidationGroups.Query.class) String requestId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long selectMaxRunNo(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationRunPO selectActiveRunForResource(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationRunPO selectLatestFinished(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<Long> selectPendingIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationRunPO> selectExpiredRunning(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("now") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant now,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationRunPO> selectPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("query") @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiOptimizationRunPageQuery query,
            @Param("offset") @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class) long offset,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long countPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("query") @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiOptimizationRunPageQuery query);

    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }
}
