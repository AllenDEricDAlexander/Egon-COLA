package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/** Persistence model for the user aggregate root. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("light_users")
public class UserPO extends EgonModel<UserPO> {

    @TableField("external_id")
    private String externalId;

    @TableField("name")
    private String name;

    @TableField("email")
    private String email;

    @TableField("status")
    private String status;
}
