package top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.LocalDateTime;

/** Persistence model for a scheduled course. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("class_course_schedules")
public class ClassCourseSchedulePO extends EgonModel<ClassCourseSchedulePO> {

    @TableField("school_class_id")
    private Long schoolClassId;

    @TableField("course_id")
    private Long courseId;

    @TableField("starts_at")
    private LocalDateTime startsAt;

    @TableField("ends_at")
    private LocalDateTime endsAt;
}
