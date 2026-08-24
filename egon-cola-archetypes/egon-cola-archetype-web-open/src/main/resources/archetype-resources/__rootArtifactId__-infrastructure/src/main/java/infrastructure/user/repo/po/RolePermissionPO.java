package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("role_permissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RolePermissionPO {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    @TableField("role_id") private Long roleId;
    @TableField("permission_id") private Long permissionId;
    @TableField("created_at") private LocalDateTime createdAt;

    public RolePermissionPO(Long id, Long roleId, Long permissionId, LocalDateTime createdAt) {
        this.id = id;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getRoleId() { return roleId; }
    public Long getPermissionId() { return permissionId; }
}
