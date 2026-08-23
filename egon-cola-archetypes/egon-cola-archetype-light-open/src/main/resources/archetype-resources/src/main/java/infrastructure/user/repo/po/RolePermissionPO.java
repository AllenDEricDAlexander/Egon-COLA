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

@TableName("role_permissions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RolePermissionPO {
    @TableField("role_code")
    private String roleCode;
    @TableField("permission_code")
    private String permissionCode;
    @TableField("granted_at")
    private Instant grantedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Key implements Serializable {
        private String roleCode;
        private String permissionCode;

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(roleCode, key.roleCode)
                    && Objects.equals(permissionCode, key.permissionCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleCode, permissionCode);
        }
    }
}
