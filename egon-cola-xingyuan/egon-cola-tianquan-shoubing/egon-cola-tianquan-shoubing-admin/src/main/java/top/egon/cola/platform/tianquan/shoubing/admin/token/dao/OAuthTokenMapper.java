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
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthTokenPO;

@Mapper
@Validated
public interface OAuthTokenMapper extends EgonColaMapper<OAuthTokenPO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthTokenPO selectByTokenDigest(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("tokenDigest") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Pattern(regexp = "[0-9a-f]{64}", groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 64, max = 64, groups = EgonColaModelValidationGroups.Query.class) String tokenDigest);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthTokenPO selectByTokenDigestForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("tokenDigest") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Pattern(regexp = "[0-9a-f]{64}", groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 64, max = 64, groups = EgonColaModelValidationGroups.Query.class) String tokenDigest);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthTokenPO> selectByAuthorization(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("authorizationPk") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long authorizationPk,
            @Param("tokenType") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            OAuthTokenTypeEnum tokenType,
            @Param("afterId") @PositiveOrZero(groups = EgonColaModelValidationGroups.Query.class) Long afterId,
            @Param("limit") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) Integer limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthTokenPO> selectExpired(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("expiresBefore") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Instant expiresBefore,
            @Param("afterId") @PositiveOrZero(groups = EgonColaModelValidationGroups.Query.class) Long afterId,
            @Param("limit") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) Integer limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthTokenPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthTokenPO> selectActiveByIds(
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
            @Valid OAuthTokenPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    int deleteVersionedById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Positive(groups = EgonColaModelValidationGroups.Delete.class) Long tenantId,
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Valid OAuthTokenPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Delete.class) Long expectedVersion);
}
