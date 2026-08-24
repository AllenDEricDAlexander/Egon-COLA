package ${package}.infrastructure.user.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("permissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PermissionPO {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private String code;
    private String name;
    private String type;
    private String status;
    @TableField("created_at") private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
