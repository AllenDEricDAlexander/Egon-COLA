package top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po;

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
@TableName("permissions")
public class PermissionPO extends EgonModel<PermissionPO> {
    @TableField("code")
    private String code;
    @TableField("name")
    private String name;
    @TableField("type")
    private String type;
    @TableField("status")
    private String status;
}
