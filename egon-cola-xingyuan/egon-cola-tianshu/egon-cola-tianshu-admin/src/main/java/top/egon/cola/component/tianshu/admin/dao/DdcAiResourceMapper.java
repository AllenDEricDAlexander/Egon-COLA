package top.egon.cola.component.tianshu.admin.dao;

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
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiKindPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiResourcePO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcAiResourceMapper extends EgonColaMapper<DdcAiResourcePO> {

    @Override
    default DdcAiResourcePO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Override
    default List<DdcAiResourcePO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    @Override
    default int restoreVersionedById(DdcAiResourcePO resource) {
        throw new UnsupportedOperationException("AI_RESOURCE_RESTORE_DISABLED");
    }

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiResourcePO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiResourcePO selectActiveForShareById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Long selectBizIdForShare(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Long selectNamespaceIdForShare(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("namespaceCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String namespaceCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Long selectEnvIdForShare(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("envCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Long selectAppIdForShare(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("appCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Long selectBindingIdForShare(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("namespaceCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String namespaceCode,
            @Param("envCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @Param("appCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiResourcePO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Collection<@Positive(groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    @Validated(EgonColaModelValidationGroups.Query.class)
    default DdcAiResourcePO selectIncludingDeletedById(Serializable id) {
        return selectIncludingDeletedById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiResourcePO selectIncludingDeletedById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiResourcePO selectForUpdateById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiResourcePO selectByBusinessKey(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("namespaceId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long namespaceId,
            @Param("envCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @Param("appCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @Param("kind") @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiKindPersistenceEnum kind,
            @Param("resourceKey") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceKey);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiResourcePO> selectPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("namespaceId") @Positive(groups = EgonColaModelValidationGroups.Query.class) Long namespaceId,
            @Param("envCode") @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @Param("appCode") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @Param("kind") DdcAiKindPersistenceEnum kind,
            @Param("resourceKeyPrefix") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class)
            String resourceKeyPrefix,
            @Param("displayName") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class)
            String displayName,
            @Param("offset") @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class) long offset,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long countPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("namespaceId") @Positive(groups = EgonColaModelValidationGroups.Query.class) Long namespaceId,
            @Param("envCode") @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @Param("appCode") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @Param("kind") DdcAiKindPersistenceEnum kind,
            @Param("resourceKeyPrefix") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class)
            String resourceKeyPrefix,
            @Param("displayName") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class)
            String displayName);

    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }
}
