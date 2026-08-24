package ${package}.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("school_class_users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolClassUserPO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("grade_id")
    private Long gradeId;

    @TableField("school_class_id")
    private Long schoolClassId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public SchoolClassUserPO(
            Long id,
            Long gradeId,
            Long schoolClassId,
            Long userId,
            LocalDateTime createdAt) {
        this.id = id;
        this.gradeId = gradeId;
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
