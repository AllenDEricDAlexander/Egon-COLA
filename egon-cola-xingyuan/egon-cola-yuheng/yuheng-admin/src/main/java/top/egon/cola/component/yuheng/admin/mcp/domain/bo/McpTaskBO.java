package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.component.yuheng.admin.mcp.repository.jdbc.JdbcMcpTaskRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.jdbc.McpJdbcJson;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpTaskBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpTaskBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpTaskBO {

    private String id;
    private String principalFingerprint;
    private String subjectId;
    private String tenantId;
    private String clientId;
    private String serverCode;
    private String toolName;
    private String requestDigest;
    private String state;
    private Map<String, Object> inputPayload;
    private Map<String, Object> resultPayload;
    private Map<String, Object> errorPayload;
    private String workerOwner;
    private Instant leaseUntil;
    private Instant executionDeadline;
    private Instant expiresAt;
    private int attemptCount;
    private int maxAttempts;
    private long revision;
    private Instant createdAt;
    private Instant updatedAt;
}
