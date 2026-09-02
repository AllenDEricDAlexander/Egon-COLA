package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("evaluation_exam")
public class ExamPO extends EgonModel<ExamPO> {
    @TableField("course_id")
    private Long courseId;
    @TableField("title")
    private String title;
    @TableField("starts_at")
    private Instant startsAt;
    @TableField("ends_at")
    private Instant endsAt;
    @TableField("status")
    private String status;
}
