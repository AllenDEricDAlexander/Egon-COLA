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

@TableName("exam")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamPo {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    @TableField("course_id") private Long courseId;
    private String title;
    @TableField("starts_at") private Instant startsAt;
    @TableField("ends_at") private Instant endsAt;
    private String status;
    @TableField("created_at") private Instant createdAt;
    @TableField("updated_at") private Instant updatedAt;

    public Long getId() { return id; } public Long getCourseId() { return courseId; }
    public String getTitle() { return title; } public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; } public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
