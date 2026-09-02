package top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po;

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
@TableName("evaluation_course_schedule")
public class CourseSchedulePO extends EgonModel<CourseSchedulePO> {
    @TableField("course_id")
    private Long courseId;
    @TableField("class_id")
    private Long classId;
    @TableField("starts_at")
    private Instant startsAt;
    @TableField("ends_at")
    private Instant endsAt;
    @TableField("status")
    private String status;
}
