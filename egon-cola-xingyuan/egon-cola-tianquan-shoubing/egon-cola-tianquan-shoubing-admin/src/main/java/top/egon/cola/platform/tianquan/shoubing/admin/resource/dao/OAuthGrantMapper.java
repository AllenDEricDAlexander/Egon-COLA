package top.egon.cola.platform.tianquan.shoubing.admin.resource.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthGrantPO;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface OAuthGrantMapper extends EgonColaMapper<OAuthGrantPO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthGrantPO selectGrant(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("clientPk") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long clientPk,
            @Param("resourceServerId") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceServerId,
            @Param("grantType") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            ResourceGrantType grantType);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthGrantPO selectGrantForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("clientPk") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long clientPk,
            @Param("resourceServerId") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceServerId,
            @Param("grantType") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            ResourceGrantType grantType);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthGrantPO> listByClient(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("clientPk") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long clientPk,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 65, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthGrantPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthGrantPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 1, max = 500, groups = EgonColaModelValidationGroups.Query.class)
            Collection<@NotNull(groups = EgonColaModelValidationGroups.Query.class)
                    @Positive(groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int updateExpectedVersion(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Positive(groups = EgonColaModelValidationGroups.Update.class) Long tenantId,
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Valid OAuthGrantPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int deleteVersionedById(
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Valid OAuthGrantPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);
}
