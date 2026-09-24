package top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentApprovalStatusEnum;

import java.time.Instant;

/**
 * 中文说明：{@code McpApprovalRecordPO} 是数据面进程自有的 {@code gateway_mcp_approval} 行模型，列集合与控制面
 * {@code top.egon.cola.component.yuheng.admin.mcp.domain.po.McpApprovalRecordPO} 逐列相同；数据面只读取与一次性消费，
 * 审批的签发与撤销始终属于控制面。技术 {@code id}、数值 {@code tenant_id}、审计/软删/乐观锁 {@code version} 来自
 * {@link EgonModel}；不可预测的审批令牌摘要保存在 {@code token_digest}（同时复制到 {@code approval_key}），
 * 调用方自报租户保存在 {@code subject_tenant_id}。
 * English summary: {@code McpApprovalRecordPO} is the data-plane process' own row model for {@code gateway_mcp_approval}
 * with a column set identical to the control plane's
 * {@code top.egon.cola.component.yuheng.admin.mcp.domain.po.McpApprovalRecordPO}; the data plane only reads and consumes
 * an approval once, while issuing and revoking stays with the control plane. The technical {@code id}, the numeric
 * {@code tenant_id}, the audit, soft-delete and optimistic-lock {@code version} come from {@link EgonModel}; the
 * unguessable approval token digest lives in {@code token_digest} (mirrored into {@code approval_key}) and the
 * caller-reported tenant in {@code subject_tenant_id}.
 *
 * 用法 / Usage: 仅在受守卫的持久边界使用；状态列绑定 {@link McpPersistentApprovalStatusEnum} 的 wire 字符串，
 * 业务 {@code revision} 与 MP {@code version}/{@code id} 互不替代。/ Use it only at the guarded persistence boundary;
 * the status column carries the wire string of {@link McpPersistentApprovalStatusEnum} and business columns such as
 * {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_mcp_approval", autoResultMap = true)
public class McpApprovalRecordPO extends EgonModel<McpApprovalRecordPO> {

    /** 中文说明：审批令牌摘要（64 位十六进制），对应 {@code token_digest}。 English summary: the 64-hex approval token digest, mapped to {@code token_digest}. */
    @TableField("token_digest")
    private String tokenDigest;

    /** 中文说明：授权主体标识，对应 {@code subject_id}。 English summary: the authorized subject identifier, mapped to {@code subject_id}. */
    @TableField("subject_id")
    private String subjectId;

    /** 中文说明：客户端标识，对应 {@code client_id}。 English summary: the client identifier, mapped to {@code client_id}. */
    @TableField("client_id")
    private String clientId;

    /** 中文说明：MCP 服务器编码，对应 {@code server_code}。 English summary: the MCP server code, mapped to {@code server_code}. */
    @TableField("server_code")
    private String serverCode;

    /** 中文说明：工具名称，对应 {@code tool_name}。 English summary: the tool name, mapped to {@code tool_name}. */
    @TableField("tool_name")
    private String toolName;

    /** 中文说明：参数摘要，对应 {@code argument_digest}。 English summary: the argument digest, mapped to {@code argument_digest}. */
    @TableField("argument_digest")
    private String argumentDigest;

    /** 中文说明：审批状态，以 {@link McpPersistentApprovalStatusEnum} 的 wire 字符串落库。 English summary: the approval status, persisted as the wire string of {@link McpPersistentApprovalStatusEnum}. */
    @TableField("status")
    private McpPersistentApprovalStatusEnum status;

    /** 中文说明：审批协议修订号，与 MP 技术 {@code version} 互不替代。 English summary: the protocol revision of the approval, never a substitute for the technical MP {@code version}. */
    @TableField("revision")
    private Long revision;

    /** 中文说明：签发时刻，对应 {@code issued_at}。 English summary: the issuing instant, mapped to {@code issued_at}. */
    @TableField("issued_at")
    private Instant issuedAt;

    /** 中文说明：有效期截止时刻，对应 {@code expires_at}。 English summary: the validity deadline, mapped to {@code expires_at}. */
    @TableField("expires_at")
    private Instant expiresAt;

    /** 中文说明：一次性消费时刻，可为空。 English summary: the one-shot consumption instant, nullable. */
    @TableField("consumed_at")
    private Instant consumedAt;

    /** 中文说明：审批协议标识，与 {@code token_digest} 同值，和 {@code tenant_id} 组成唯一键。 English summary: the protocol approval identifier, equal to {@code token_digest}, unique together with {@code tenant_id}. */
    @TableField("approval_key")
    private String approvalKey;

    /** 中文说明：原 MCP owner 的字符串租户标识；不与部署持久化的数值 {@code tenant_id} 混用。 English summary: the original MCP owner tenant string; it is not mixed with the deployment-persisted numeric {@code tenant_id}. */
    @TableField("subject_tenant_id")
    private String subjectTenantId;
}
