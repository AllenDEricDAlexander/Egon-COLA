package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/** Persistence model for the user-to-role link. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("user_roles")
public class UserRolePO extends EgonModel<UserRolePO> {

    @TableField("user_id")
    private Long userId;

    @TableField("role_code")
    private String roleCode;
}
