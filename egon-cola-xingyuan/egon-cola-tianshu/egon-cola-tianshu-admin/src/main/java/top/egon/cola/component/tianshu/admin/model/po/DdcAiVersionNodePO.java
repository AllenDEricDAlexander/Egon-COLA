package top.egon.cola.component.tianshu.admin.model.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiNodePersistenceEnum;

import java.nio.charset.StandardCharsets;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ddc_ai_version_node", autoResultMap = true)
public class DdcAiVersionNodePO extends EgonModel<DdcAiVersionNodePO> {

    @TableField(value = "version_id", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long versionId;

    @TableField(value = "parent_path", updateStrategy = FieldStrategy.NEVER)
    @Size(max = 1024, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String parentPath;

    @TableField(value = "path", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 1024, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String path;

    @TableField(value = "node_type", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private DdcAiNodePersistenceEnum nodeType;

    @TableField(value = "content", updateStrategy = FieldStrategy.NEVER)
    @Size(max = 262144, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String content;

    @TableField(value = "content_sha256", updateStrategy = FieldStrategy.NEVER)
    @Pattern(regexp = "^[0-9a-f]{64}$", groups = {
            EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String contentSha256;

    @TableField(value = "content_bytes", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Min(value = 0, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long contentBytes;

    @AssertTrue(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    public boolean isPathDefinitionValid() {
        if (path == null || nodeType == null) return true;
        if (path.getBytes(StandardCharsets.UTF_8).length > 1024
                || (parentPath != null && parentPath.getBytes(StandardCharsets.UTF_8).length > 1024)) return false;
        if (path.isEmpty()) return parentPath == null && nodeType == DdcAiNodePersistenceEnum.DIRECTORY;
        if (parentPath == null) return false;
        return parentPath.equals(path.replaceFirst("(^|/)[^/]+$", ""));
    }

    @AssertTrue(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    public boolean isContentDefinitionValid() {
        if (nodeType == null || contentBytes == null) return true;
        if (nodeType == DdcAiNodePersistenceEnum.DIRECTORY) {
            return content == null && contentSha256 == null && contentBytes == 0;
        }
        if (content == null || contentSha256 == null || !contentSha256.matches("^[0-9a-f]{64}$")) return false;
        long actualBytes = content.getBytes(StandardCharsets.UTF_8).length;
        return contentBytes == actualBytes && actualBytes <= 262144;
    }
}
