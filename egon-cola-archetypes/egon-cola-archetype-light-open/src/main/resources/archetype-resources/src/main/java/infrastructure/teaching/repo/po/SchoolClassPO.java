package ${package}.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/** Persistence model for a school class. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("light_school_classes")
public class SchoolClassPO extends EgonModel<SchoolClassPO> {

    @TableField("name")
    private String name;

    @TableField("semester")
    private String semester;

    @TableField("status")
    private String status;
}
