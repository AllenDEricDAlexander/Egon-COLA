package top.egon.cola.component.common.mybatis.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Common persistence fields for Egon COLA repositories; entities perform no persistence operations.
 * We did not choose to implement 'extends Model<M>' here, and abandoned the AR mode
 * @param <M> concrete self type retained for repository generic compatibility
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
public abstract class EgonModel<M extends EgonModel<M>> {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(groups = {EgonColaModelValidationGroups.Persisted.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Delete.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class,
            EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Delete.class})
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT_UPDATE, updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    private Long tenantId;

    @TableField(value = "create_user_id", fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    private String createUserId;

    @TableField(value = "create_time", fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    private Instant createTime;

    @TableField(value = "update_user_id", fill = FieldFill.INSERT_UPDATE)
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    private String updateUserId;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    private Instant updateTime;

    @TableField(value = "deleted_at", fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @TableLogic(value = "null", delval = "(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')")
    @Null(groups = {EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Delete.class})
    private LocalDateTime deletedAt;

    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    @NotNull(groups = {EgonColaModelValidationGroups.Persisted.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Delete.class})
    @Min(value = 0, groups = {EgonColaModelValidationGroups.Persisted.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Delete.class})
    private Long version;
}
