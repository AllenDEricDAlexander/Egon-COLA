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

@TableName("users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserPO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    @TableField("external_id")
    private String externalId;
    @TableField("name")
    private String name;
    @TableField("email")
    private String email;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private Instant createdAt;
}
