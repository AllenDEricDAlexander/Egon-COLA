package top.egon.cola.component.yuheng.admin.mcp.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Instant;

/**
 * 中文说明：{@code McpApprovalRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_mcp_approval} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code McpApprovalRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_mcp_approval} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_mcp_approval", autoResultMap = true)
public class McpApprovalRecordPO extends EgonModel<McpApprovalRecordPO> {

    @TableField("token_digest")
    private String tokenDigest;

    @TableField("subject_id")
    private String subjectId;

    @TableField("client_id")
    private String clientId;

    @TableField("server_code")
    private String serverCode;

    @TableField("tool_name")
    private String toolName;

    @TableField("argument_digest")
    private String argumentDigest;

    @TableField("status")
    private String status;

    @TableField("revision")
    private Long revision;

    @TableField("issued_at")
    private Instant issuedAt;

    @TableField("expires_at")
    private Instant expiresAt;

    @TableField("consumed_at")
    private Instant consumedAt;

    @TableField("approval_key")
    private String approvalKey;

    @TableField("subject_tenant_id")
    private String subjectTenantId;
}
