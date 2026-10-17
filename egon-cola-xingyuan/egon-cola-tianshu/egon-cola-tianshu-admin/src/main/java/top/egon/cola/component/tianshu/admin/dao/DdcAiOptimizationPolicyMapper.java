package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationPolicyPO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 优化策略的租户内访问。 / Tenant-scoped optimization-policy access. */
@Mapper
@Validated
public interface DdcAiOptimizationPolicyMapper extends EgonColaMapper<DdcAiOptimizationPolicyPO> {

    @Override
    default DdcAiOptimizationPolicyPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Override
    default List<DdcAiOptimizationPolicyPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    @Override
    default int restoreVersionedById(DdcAiOptimizationPolicyPO policy) {
        throw new UnsupportedOperationException("AI_OPTIMIZATION_POLICY_RESTORE_DISABLED");
    }

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationPolicyPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationPolicyPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Collection<@Positive(groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationPolicyPO selectActiveByResourceId(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiOptimizationPolicyPO selectActiveByResourceIdForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    /** 锁定可评估策略；须在事务内调用。 / Locks evaluable policies; must run inside a transaction. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiOptimizationPolicyPO> selectEvaluableForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<Long> selectJudgeEnabledResourceIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId);

    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }
}
