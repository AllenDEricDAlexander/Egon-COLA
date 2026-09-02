package top.egon.cola.archetype.source.web.infrastructure.teaching.repo.po;

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
@TableName("school_class_users")
public class SchoolClassUserPO extends EgonModel<SchoolClassUserPO> {
    @TableField("user_id")
    private Long userId;
    @TableField("grade_id")
    private Long gradeId;
    @TableField("school_class_id")
    private Long schoolClassId;
}
