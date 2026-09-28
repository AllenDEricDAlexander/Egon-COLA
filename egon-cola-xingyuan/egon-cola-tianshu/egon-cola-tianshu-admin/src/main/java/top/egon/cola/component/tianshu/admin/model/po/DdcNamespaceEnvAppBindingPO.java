package top.egon.cola.component.tianshu.admin.model.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ddc_namespace_env_app", autoResultMap = true)
public class DdcNamespaceEnvAppBindingPO extends EgonModel<DdcNamespaceEnvAppBindingPO> {

    @TableField(value = "public_id", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 64, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    private String publicId;

    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long namespaceId;

    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 32, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    private String envCode;

    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long appId;

    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Boolean enabled;

    @TableField("created_at")
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String bizCode;
    @TableField(exist = false)
    private String namespaceCode;
    @TableField(exist = false)
    private String namespacePublicId;
    @TableField(exist = false)
    private String appCode;
    @TableField(exist = false)
    private String appName;
    @TableField(exist = false)
    private String appPublicId;
}
