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

@TableName("exam_paper")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamPaperPo {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    @TableField("exam_id") private Long examId;
    private String title;
    @TableField("total_points") private int totalPoints;
    private String status;
    @TableField("created_at") private Instant createdAt;
    @TableField("updated_at") private Instant updatedAt;
    public Long getId() { return id; } public Long getExamId() { return examId; }
    public String getTitle() { return title; } public int getTotalPoints() { return totalPoints; }
    public String getStatus() { return status; } public Instant getCreatedAt() { return createdAt; }
    public void update(String title, int totalPoints, String status, Instant updatedAt) {
        this.title = title;
        this.totalPoints = totalPoints;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
