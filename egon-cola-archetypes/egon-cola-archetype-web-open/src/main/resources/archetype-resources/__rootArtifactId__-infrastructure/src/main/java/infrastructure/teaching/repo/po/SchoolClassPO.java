package ${package}.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("school_classes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SchoolClassPO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String name;

    @TableField("grade_name")
    private String gradeName;

    @TableField("grade_id")
    private Long gradeId;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGradeName() {
        return gradeName;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
