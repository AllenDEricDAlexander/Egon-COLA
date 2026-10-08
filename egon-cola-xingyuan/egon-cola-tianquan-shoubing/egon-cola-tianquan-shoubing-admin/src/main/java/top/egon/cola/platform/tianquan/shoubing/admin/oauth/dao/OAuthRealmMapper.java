package top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthRealmPO;

import java.util.List;

@Mapper
@Validated
public interface OAuthRealmMapper extends EgonColaMapper<OAuthRealmPO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthRealmPO selectByIssuer(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("issuer") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 2048, groups = EgonColaModelValidationGroups.Query.class) String issuer);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthRealmPO selectByRealmKey(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("realmKey") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String realmKey);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthRealmPO selectByRealmKeyForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("realmKey") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String realmKey);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthRealmPO> selectDirectory(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int updateExpectedVersion(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Positive(groups = EgonColaModelValidationGroups.Update.class) Long tenantId,
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Valid OAuthRealmPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Update.class) Long expectedVersion);
}
