package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@TableName("permissions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PermissionPO {
    @TableId(value = "code", type = IdType.INPUT)
    private String code;
    @TableField("name")
    private String name;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private Instant createdAt;
}
