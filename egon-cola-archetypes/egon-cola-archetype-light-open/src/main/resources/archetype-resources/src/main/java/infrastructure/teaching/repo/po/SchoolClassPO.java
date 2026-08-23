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

@TableName("school_classes")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SchoolClassPO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    @TableField("name")
    private String name;
    @TableField("semester")
    private String semester;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private Instant createdAt;
}
