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
@TableName("school_classes")
public class SchoolClassPO extends EgonModel<SchoolClassPO> {
    @TableField("name")
    private String name;
    @TableField("grade_name")
    private String gradeName;
    @TableField("grade_id")
    private Long gradeId;
    @TableField("status")
    private String status;
}
