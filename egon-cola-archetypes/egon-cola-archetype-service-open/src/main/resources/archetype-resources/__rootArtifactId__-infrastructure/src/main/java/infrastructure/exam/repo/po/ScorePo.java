#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("score")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScorePo {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    @TableField("exam_id") private Long examId;
    @TableField("course_id") private Long courseId;
    @TableField("student_id") private Long studentId;
    private int points;
    private String status;
    @TableField("created_at") private Instant createdAt;
    @TableField("updated_at") private Instant updatedAt;
    public Long getId() { return id; } public Long getExamId() { return examId; }
    public Long getCourseId() { return courseId; } public Long getStudentId() { return studentId; }
    public int getPoints() { return points; } public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(
            Long courseId, Long studentId, int points, String status, Instant updatedAt) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.points = points;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
