package top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.typehandler.McpJsonbTypeHandler;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentTaskStateEnum;

import java.time.Instant;

/**
 * 中文说明：{@code McpTaskRecordPO} 是数据面进程自有的 {@code gateway_mcp_task_instance} 行模型，列集合与控制面
 * {@code top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskRecordPO} 逐列相同（两进程共享同一张表，禁止各自增删列）；
 * 技术身份 {@code id}、租户 {@code tenant_id}、审计与软删、乐观锁 {@code version} 全部来自 {@link EgonModel}。
 * 协议任务标识保存在 {@code task_key}，调用方自报租户保存在 {@code subject_tenant_id}，二者与部署绑定的
 * {@code tenant_id} 互不替代。
 * English summary: {@code McpTaskRecordPO} is the data-plane process' own row model for {@code gateway_mcp_task_instance};
 * its column set is identical to the control plane's
 * {@code top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskRecordPO} because both processes share one table and
 * neither may add or drop columns on its own. The technical {@code id}, the tenant {@code tenant_id}, the audit and
 * soft-delete columns and the optimistic-lock {@code version} all come from {@link EgonModel}. The protocol task identity
 * lives in {@code task_key} and the caller-reported tenant in {@code subject_tenant_id}; neither substitutes for the
 * deployment-bound {@code tenant_id}.
 *
 * 用法 / Usage: 只在受守卫的持久边界使用；JSON 载体列为 {@code JsonNode} 并显式绑定
 * {@link McpJsonbTypeHandler}，生命周期状态列绑定 {@link McpPersistentTaskStateEnum} 的 {@code wireValue}。
 * / Use it only at the guarded persistence boundary; the JSON carrier columns are {@code JsonNode} bound explicitly to
 * {@link McpJsonbTypeHandler} and the lifecycle state column carries {@link McpPersistentTaskStateEnum}'s
 * {@code wireValue}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_mcp_task_instance", autoResultMap = true)
public class McpTaskRecordPO extends EgonModel<McpTaskRecordPO> {

    /** 中文说明：主体不可逆指纹，对应 {@code principal_fingerprint}。 English summary: the irreversible principal fingerprint, mapped to {@code principal_fingerprint}. */
    @TableField("principal_fingerprint")
    private String principalFingerprint;

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

    /** 中文说明：规范化请求摘要，对应 {@code request_digest}。 English summary: the canonical request digest, mapped to {@code request_digest}. */
    @TableField("request_digest")
    private String requestDigest;

    /** 中文说明：任务生命周期状态，以 {@link McpPersistentTaskStateEnum} 的 wire 字符串落库。 English summary: the task lifecycle state, persisted as the wire string of {@link McpPersistentTaskStateEnum}. */
    @TableField("state")
    private McpPersistentTaskStateEnum state;

    /** 中文说明：输入载荷 JSONB 列，可为空。 English summary: the nullable input payload carried as a JSONB column. */
    @TableField(value = "input_payload", typeHandler = McpJsonbTypeHandler.class)
    private JsonNode inputPayload;

    /** 中文说明：结果载荷 JSONB 列，可为空。 English summary: the nullable result payload carried as a JSONB column. */
    @TableField(value = "result_payload", typeHandler = McpJsonbTypeHandler.class)
    private JsonNode resultPayload;

    /** 中文说明：错误载荷 JSONB 列，可为空。 English summary: the nullable error payload carried as a JSONB column. */
    @TableField(value = "error_payload", typeHandler = McpJsonbTypeHandler.class)
    private JsonNode errorPayload;

    /** 中文说明：当前工作者占有者，可为空。 English summary: the current worker owner, nullable. */
    @TableField("worker_owner")
    private String workerOwner;

    /** 中文说明：租约到期时刻，可为空。 English summary: the lease expiry instant, nullable. */
    @TableField("lease_until")
    private Instant leaseUntil;

    /** 中文说明：执行截止时间，对应 {@code execution_deadline}。 English summary: the execution deadline, mapped to {@code execution_deadline}. */
    @TableField("execution_deadline")
    private Instant executionDeadline;

    /** 中文说明：任务记录保留到期时刻，对应 {@code expires_at}。 English summary: the retention expiry instant, mapped to {@code expires_at}. */
    @TableField("expires_at")
    private Instant expiresAt;

    /** 中文说明：已消耗的执行尝试次数，对应 {@code attempt_count}。 English summary: the consumed attempt counter, mapped to {@code attempt_count}. */
    @TableField("attempt_count")
    private Integer attemptCount;

    /** 中文说明：允许的最大尝试次数，对应 {@code max_attempts}。 English summary: the allowed attempt ceiling, mapped to {@code max_attempts}. */
    @TableField("max_attempts")
    private Integer maxAttempts;

    /** 中文说明：任务协议修订号，与 MP 技术 {@code version} 互不替代。 English summary: the protocol revision of the task, never a substitute for the technical MP {@code version}. */
    @TableField("revision")
    private Long revision;

    /** 中文说明：不可预测的协议任务标识（43 字符 base64url），与 {@code tenant_id} 组成唯一键。 English summary: the unguessable protocol task identifier (43-char base64url), unique together with {@code tenant_id}. */
    @TableField("task_key")
    private String taskKey;

    /** 中文说明：原 MCP owner 的字符串租户标识；不与部署持久化的数值 {@code tenant_id} 混用。 English summary: the original MCP owner tenant string; it is not mixed with the deployment-persisted numeric {@code tenant_id}. */
    @TableField("subject_tenant_id")
    private String subjectTenantId;
}
