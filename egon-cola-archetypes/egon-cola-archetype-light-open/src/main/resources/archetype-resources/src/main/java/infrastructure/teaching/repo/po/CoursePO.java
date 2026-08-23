package ${package}.infrastructure.teaching.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("courses")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CoursePO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    @TableField("course_code")
    private String courseCode;
    @TableField("name")
    private String name;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private Instant createdAt;
}
