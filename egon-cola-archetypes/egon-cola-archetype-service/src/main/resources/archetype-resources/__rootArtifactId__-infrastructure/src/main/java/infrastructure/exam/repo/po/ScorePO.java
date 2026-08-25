#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.po;

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
@TableName("evaluation_score")
public class ScorePO extends EgonModel<ScorePO> {
    @TableField("exam_id")
    private Long examId;
    @TableField("course_id")
    private Long courseId;
    @TableField("student_id")
    private Long studentId;
    @TableField("points")
    private int points;
    @TableField("status")
    private String status;
}
