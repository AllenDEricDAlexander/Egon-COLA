package [=poPackage];

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("[=tableName]")
public class [=poType] extends EgonModel<[=poType]> {
[#list businessFields as field]
[#if field.required]
    @NotNull(groups = {[#if !field.hasDefault]EgonColaModelValidationGroups.Insert.class, [/#if]EgonColaModelValidationGroups.Persisted.class})
[/#if]
[#if field.length?has_content && field.javaType == "String"]
    @Size(max = [=field.length], groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Query.class, EgonColaModelValidationGroups.Persisted.class})
[/#if]
[#if field.enumPattern?has_content && field.javaType == "String"]
    @Pattern(regexp = "[=field.enumPattern]", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Query.class, EgonColaModelValidationGroups.Persisted.class})
[/#if]
    @TableField("[=field.column]")
    private [=field.javaType] [=field.javaName];
[/#list]
}
