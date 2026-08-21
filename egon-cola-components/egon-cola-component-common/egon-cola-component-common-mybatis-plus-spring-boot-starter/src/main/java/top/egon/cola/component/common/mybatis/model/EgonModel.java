package top.egon.cola.component.common.mybatis.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Common ActiveRecord persistence model for Egon COLA repositories.
 *
 * @param <M> concrete self type used by MyBatis-Plus ActiveRecord wrappers
 */
public abstract class EgonModel<M extends EgonModel<M>> extends Model<M> {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
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

    @TableField(value = "is_deleted", fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @TableLogic(value = "0", delval = "1")
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    private Boolean isDeleted;

    @Override
    public final boolean insert() {
        beforeInsert();
        boolean result = super.insert();
        afterInsert(result);
        return result;
    }

    @Override
    public final boolean deleteById(Serializable id) {
        Objects.requireNonNull(id, "id must not be null");
        beforeDelete();
        boolean result = super.deleteById(id);
        afterDelete(result);
        return result;
    }

    @Override
    public final boolean deleteById() {
        requireId();
        beforeDelete();
        boolean result = super.deleteById();
        afterDelete(result);
        return result;
    }

    @Override
    public final boolean delete(Wrapper<M> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        beforeDelete();
        boolean result = super.delete(wrapper);
        afterDelete(result);
        return result;
    }

    @Override
    public final boolean updateById() {
        requireId();
        beforeUpdate();
        boolean result = super.updateById();
        afterUpdate(result);
        return result;
    }

    @Override
    public final boolean update(Wrapper<M> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        beforeUpdate();
        boolean result = super.update(wrapper);
        afterUpdate(result);
        return result;
    }

    @Override
    public final Serializable pkVal() {
        return id;
    }

    protected void beforeInsert() {
    }

    protected void afterInsert(boolean result) {
    }

    protected void beforeUpdate() {
    }

    protected void afterUpdate(boolean result) {
    }

    protected void beforeDelete() {
    }

    protected void afterDelete(boolean result) {
    }

    private Long requireId() {
        return Objects.requireNonNull(id, "id must not be null");
    }

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

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
