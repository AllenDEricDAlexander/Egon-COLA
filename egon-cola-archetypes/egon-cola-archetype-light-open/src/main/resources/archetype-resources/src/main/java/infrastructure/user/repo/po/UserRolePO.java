package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@TableName("user_roles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserRolePO {
    @TableField("user_id")
    private Long userId;
    @TableField("role_code")
    private String roleCode;
    @TableField("assigned_at")
    private Instant assignedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Key implements Serializable {
        private Long userId;
        private String roleCode;

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(roleCode, key.roleCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleCode);
        }
    }
}
