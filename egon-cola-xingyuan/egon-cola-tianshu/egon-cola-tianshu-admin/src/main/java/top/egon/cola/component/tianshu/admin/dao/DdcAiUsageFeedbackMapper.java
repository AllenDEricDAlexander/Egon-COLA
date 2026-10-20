package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiScoreAggregatePO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiUsageFeedbackPO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 使用样本的租户内访问；所有语句显式带 tenantId。 / Tenant-scoped usage-sample access; every statement carries tenantId. */
@Mapper
@Validated
public interface DdcAiUsageFeedbackMapper extends EgonColaMapper<DdcAiUsageFeedbackPO> {

    @Override
    default DdcAiUsageFeedbackPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Override
    default List<DdcAiUsageFeedbackPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    @Override
    default int restoreVersionedById(DdcAiUsageFeedbackPO feedback) {
        throw new UnsupportedOperationException("AI_USAGE_FEEDBACK_RESTORE_DISABLED");
    }

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiUsageFeedbackPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiUsageFeedbackPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Collection<@Positive(groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    /** 认领待评分样本并加行锁；须在事务内调用。 / Claims pending samples with row locks; must run inside a transaction. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiUsageFeedbackPO> selectClaimableForJudge(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceIds") @NotEmpty(groups = EgonColaModelValidationGroups.Query.class) Collection<@Positive(groups = EgonColaModelValidationGroups.Query.class) Long> resourceIds,
            @Param("now") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant now,
            @Param("maxAttempts") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 10, groups = EgonColaModelValidationGroups.Query.class) int maxAttempts,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiScoreAggregatePO selectScoreAggregate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @Param("versionId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long versionId,
            @Param("windowStart") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowStart,
            @Param("windowEnd") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowEnd);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiUsageFeedbackPO> selectWorst(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @Param("versionId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long versionId,
            @Param("windowStart") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowStart,
            @Param("windowEnd") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant windowEnd,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 200, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiUsageFeedbackPO> selectPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("query") @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiUsageFeedbackPageQuery query,
            @Param("offset") @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class) long offset,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long countPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("query") @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiUsageFeedbackPageQuery query);

    /** 锁定待清理的过期样本 ID。 / Locks the IDs of expired samples awaiting retention cleanup. */
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<Long> selectExpiredIdsForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("before") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant before,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 5000, groups = EgonColaModelValidationGroups.Query.class) int limit);

    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }
}
