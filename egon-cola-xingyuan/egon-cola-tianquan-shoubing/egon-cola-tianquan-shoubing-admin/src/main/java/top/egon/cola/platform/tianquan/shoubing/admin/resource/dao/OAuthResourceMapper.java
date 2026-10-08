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
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthResourcePO;

import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface OAuthResourceMapper extends EgonColaMapper<OAuthResourcePO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthResourcePO> selectDirectory(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 10001, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthResourcePO selectByResourceServerId(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceServerId") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceServerId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthResourcePO selectByResourceServerIdForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceServerId") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceServerId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthResourcePO selectByResourceUri(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("resourceUri") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 2048, groups = EgonColaModelValidationGroups.Query.class) String resourceUri);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthResourcePO selectBySource(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("appCode") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @Param("environment") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String environment);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthResourcePO selectByManagementClientPk(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("managementClientPk") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long managementClientPk);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthResourcePO> selectByBizCodeAndEnvironmentAndAppCodeIn(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("environment") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String environment,
            @Param("appCodes") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 1, max = 100, groups = EgonColaModelValidationGroups.Query.class)
            Collection<@NotBlank(groups = EgonColaModelValidationGroups.Query.class)
                    @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String> appCodes);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthResourcePO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthResourcePO> selectActiveByIds(
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
            @Valid OAuthResourcePO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int deleteVersionedById(
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Valid OAuthResourcePO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);
}
