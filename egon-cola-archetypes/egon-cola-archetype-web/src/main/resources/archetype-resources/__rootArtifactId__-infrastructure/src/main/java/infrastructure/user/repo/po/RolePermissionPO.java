package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("role_permissions")
public class RolePermissionPO extends EgonModel<RolePermissionPO> {
    @TableField("role_id")
    private Long roleId;
    @TableField("permission_id")
    private Long permissionId;
}
