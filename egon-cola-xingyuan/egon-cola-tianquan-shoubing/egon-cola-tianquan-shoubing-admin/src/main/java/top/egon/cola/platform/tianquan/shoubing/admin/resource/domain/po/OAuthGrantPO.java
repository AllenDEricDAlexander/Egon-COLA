package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthStringListJsonbTypeHandler;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oauth_grant", schema = "tianquan_oauth", autoResultMap = true)
public class OAuthGrantPO extends EgonModel<OAuthGrantPO> {

    @TableField(value = "client_pk", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    private Long clientPk;

    @TableField(value = "resource_server_id", updateStrategy = FieldStrategy.NEVER)
    @NotBlank(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 128, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    private String resourceServerId;

    @TableField(value = "grant_type", updateStrategy = FieldStrategy.NEVER)
    @NotBlank(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Pattern(regexp = "USER_DELEGATION|CLIENT_CREDENTIALS", groups = {
            EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 32, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Persisted.class})
    private String grantType;

    @TableField(value = "allowed_scopes", typeHandler = OAuthStringListJsonbTypeHandler.class)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 64, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    private List<@NotBlank @Size(max = 256) String> allowedScopes;

    @TableField("status")
    @NotBlank(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Pattern(regexp = "ACTIVE|DISABLED", groups = {
            EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    private String status;

    @TableField("resource_version")
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    @PositiveOrZero(groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    private Long resourceVersion;
}
