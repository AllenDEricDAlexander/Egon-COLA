package top.egon.cola.component.yuheng.admin.mcp.domain.po;

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
import top.egon.cola.component.yuheng.admin.shared.dao.typehandler.GatewayJsonbTypeHandler;

import java.time.Instant;

/**
 * 中文说明：{@code McpTaskRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_mcp_task_instance} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code McpTaskRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_mcp_task_instance} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式绑定 {@code GatewayJsonbTypeHandler}、vector 列绑定 {@code GatewayVectorTypeHandler}，并依赖 {@code autoResultMap} 读回；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; jsonb columns bind {@code GatewayJsonbTypeHandler} and vector columns {@code GatewayVectorTypeHandler} under {@code autoResultMap}; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_mcp_task_instance", autoResultMap = true)
public class McpTaskRecordPO extends EgonModel<McpTaskRecordPO> {

    @TableField("principal_fingerprint")
    private String principalFingerprint;

    @TableField("subject_id")
    private String subjectId;

    @TableField("client_id")
    private String clientId;

    @TableField("server_code")
    private String serverCode;

    @TableField("tool_name")
    private String toolName;

    @TableField("request_digest")
    private String requestDigest;

    @TableField("state")
    private String state;

    @TableField(value = "input_payload", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode inputPayload;

    @TableField(value = "result_payload", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode resultPayload;

    @TableField(value = "error_payload", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode errorPayload;

    @TableField("worker_owner")
    private String workerOwner;

    @TableField("lease_until")
    private Instant leaseUntil;

    @TableField("execution_deadline")
    private Instant executionDeadline;

    @TableField("expires_at")
    private Instant expiresAt;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("max_attempts")
    private Integer maxAttempts;

    @TableField("revision")
    private Long revision;

    @TableField("task_key")
    private String taskKey;

    @TableField("subject_tenant_id")
    private String subjectTenantId;
}
