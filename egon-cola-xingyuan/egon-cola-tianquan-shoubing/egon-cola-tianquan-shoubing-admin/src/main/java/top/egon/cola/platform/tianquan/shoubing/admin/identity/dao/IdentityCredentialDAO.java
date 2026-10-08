package top.egon.cola.platform.tianquan.shoubing.admin.identity.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityCredentialPO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface IdentityCredentialDAO extends EgonColaMapper<IdentityCredentialPO> {

    @Override
    @Validated(EgonColaModelValidationGroups.Query.class)
    default IdentityCredentialPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireId(id));
    }

    IdentityCredentialPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Override
    @Validated(EgonColaModelValidationGroups.Query.class)
    default List<IdentityCredentialPO> selectActiveByIds(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    List<IdentityCredentialPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Collection<@NotNull @Positive Long> ids);

    IdentityCredentialPO selectByExternalKey(
            @Param("directoryPartitionId") @NotNull @Positive Long directoryPartitionId,
            @Param("identitySub") @NotBlank @Size(max = 64) String identitySub);

    IdentityCredentialPO selectForUpdate(
            @Param("directoryPartitionId") @NotNull @Positive Long directoryPartitionId,
            @Param("identitySub") @NotBlank @Size(max = 64) String identitySub);

    List<IdentityCredentialPO> selectDirectory(
            @Param("directoryPartitionId") @NotNull @Positive Long directoryPartitionId);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int updateExpectedVersion(
            @Param("directoryPartitionId") @NotNull @Positive Long directoryPartitionId,
            @Param("po") @NotNull @Valid IdentityCredentialPO entity,
            @Param("expectedVersion") @NotNull @PositiveOrZero Long expectedVersion);

    private static Long requireId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0L) {
            throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        }
        return value;
    }
}
