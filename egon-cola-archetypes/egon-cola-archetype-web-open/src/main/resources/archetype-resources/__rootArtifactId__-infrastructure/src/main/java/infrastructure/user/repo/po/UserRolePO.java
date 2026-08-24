package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("user_roles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRolePO {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    @TableField("user_id") private Long userId;
    @TableField("role_id") private Long roleId;
    @TableField("created_at") private LocalDateTime createdAt;

    public UserRolePO(Long id, Long userId, Long roleId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getRoleId() { return roleId; }
}
