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
@TableName("users")
public class UserPO extends EgonModel<UserPO> {
    @TableField("name")
    private String name;
    @TableField("email")
    private String email;
    @TableField("status")
    private String status;
}
