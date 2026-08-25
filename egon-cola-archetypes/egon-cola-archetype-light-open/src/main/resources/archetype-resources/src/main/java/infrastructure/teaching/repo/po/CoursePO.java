package ${package}.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/** Persistence model for a course. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("light_courses")
public class CoursePO extends EgonModel<CoursePO> {

    @TableField("course_code")
    private String courseCode;

    @TableField("name")
    private String name;

    @TableField("status")
    private String status;
}
