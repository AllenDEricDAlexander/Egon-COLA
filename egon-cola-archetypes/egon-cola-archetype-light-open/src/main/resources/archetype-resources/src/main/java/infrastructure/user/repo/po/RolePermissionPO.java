package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/** Persistence model for the role-to-permission link. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("light_role_permissions")
public class RolePermissionPO extends EgonModel<RolePermissionPO> {

    @TableField("role_code")
    private String roleCode;

    @TableField("permission_code")
    private String permissionCode;
}
