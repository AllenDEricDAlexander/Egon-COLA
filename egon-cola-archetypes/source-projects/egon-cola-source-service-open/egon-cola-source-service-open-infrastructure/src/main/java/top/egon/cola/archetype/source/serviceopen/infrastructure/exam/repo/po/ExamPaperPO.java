package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.po;

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
@TableName("evaluation_exam_paper")
public class ExamPaperPO extends EgonModel<ExamPaperPO> {
    @TableField("exam_id")
    private Long examId;
    @TableField("title")
    private String title;
    @TableField("total_points")
    private int totalPoints;
    @TableField("status")
    private String status;
}
