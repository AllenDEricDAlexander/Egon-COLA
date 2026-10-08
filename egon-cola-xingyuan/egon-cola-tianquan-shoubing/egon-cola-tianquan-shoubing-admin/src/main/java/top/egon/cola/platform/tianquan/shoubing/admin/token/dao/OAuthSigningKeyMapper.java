package top.egon.cola.platform.tianquan.shoubing.admin.token.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSigningKeyPO;

@Mapper
@Validated
public interface OAuthSigningKeyMapper extends EgonColaMapper<OAuthSigningKeyPO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthSigningKeyPO selectByKid(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("kid") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Pattern(regexp = "[A-Za-z0-9_-]{1,128}", groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String kid);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthSigningKeyPO selectByKidForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("kid") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Pattern(regexp = "[A-Za-z0-9_-]{1,128}", groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String kid);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthSigningKeyPO> selectByStatuses(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("statuses") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 1, max = 5, groups = EgonColaModelValidationGroups.Query.class)
            Collection<@NotNull(groups = EgonColaModelValidationGroups.Query.class) OAuthKeyStatusEnum> statuses,
            @Param("afterId") @PositiveOrZero(groups = EgonColaModelValidationGroups.Query.class) Long afterId,
            @Param("limit") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 8, groups = EgonColaModelValidationGroups.Query.class) Integer limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthSigningKeyPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthSigningKeyPO> selectActiveByIds(
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
            @Valid OAuthSigningKeyPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    int deleteVersionedById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Positive(groups = EgonColaModelValidationGroups.Delete.class) Long tenantId,
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Valid OAuthSigningKeyPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Delete.class) Long expectedVersion);
}
