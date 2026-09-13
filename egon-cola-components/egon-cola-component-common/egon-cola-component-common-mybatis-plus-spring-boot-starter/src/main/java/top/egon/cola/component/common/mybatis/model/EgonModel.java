package top.egon.cola.component.common.mybatis.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Null;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Common persistence fields for Egon COLA repositories; entities perform no persistence operations.
 *
 * @param <M> concrete self type retained for repository generic compatibility
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
