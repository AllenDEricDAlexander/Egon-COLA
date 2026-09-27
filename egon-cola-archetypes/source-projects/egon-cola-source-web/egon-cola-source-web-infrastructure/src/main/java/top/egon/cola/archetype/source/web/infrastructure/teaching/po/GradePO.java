package top.egon.cola.archetype.source.web.infrastructure.teaching.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("grades")
public class GradePO extends EgonModel<GradePO> {
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 160, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Query.class, EgonColaModelValidationGroups.Persisted.class})
    @TableField("code")
    private String code;
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 120, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Query.class, EgonColaModelValidationGroups.Persisted.class})
    @TableField("name")
    private String name;
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 32, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Query.class, EgonColaModelValidationGroups.Persisted.class})
    @TableField("status")
    private String status;
}
