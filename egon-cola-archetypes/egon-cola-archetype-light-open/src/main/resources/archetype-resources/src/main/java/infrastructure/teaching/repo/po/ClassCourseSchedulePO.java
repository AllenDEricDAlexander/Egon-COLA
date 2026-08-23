package ${package}.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@TableName("class_course_schedules")
@NoArgsConstructor
@Getter
@Setter
public class ClassCourseSchedulePO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    @TableField("school_class_id")
    private Long schoolClassId;
    @TableField("course_id")
    private Long courseId;
    @TableField("starts_at")
    private LocalDateTime startsAt;
    @TableField("ends_at")
    private LocalDateTime endsAt;
    @TableField("created_at")
    private Instant createdAt;

    public ClassCourseSchedulePO(
            Long id,
            Long schoolClassId,
            Long courseId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Instant createdAt) {
        this.id = id;
        this.schoolClassId = schoolClassId;
        this.courseId = courseId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

}
