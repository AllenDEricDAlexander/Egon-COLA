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

    /**
     * 中文说明：按原持久化载体的紧凑构造器契约建立异步任务载体；Lombok 规范构造器不承担这些校验，故持久化读写边界必须经此工厂。
     * English summary: Builds an async task carrier enforcing the invariants of the legacy persistence carrier's compact constructor; Lombok's canonical constructor cannot, so persistence read/write boundaries must go through this factory.
     *
     * 用法 / Usage: 由任务仓储在创建与载入时调用；/ Call it from the task repository when creating or loading a task.
     * @param id 参数 id；parameter id。
     * @param principalFingerprint 参数 主体指纹；parameter principal fingerprint。
     * @param subjectId 参数 主体Id；parameter subject id。
     * @param tenantId 参数 租户Id；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具名称；parameter tool name。
     * @param requestDigest 参数 请求摘要；parameter request digest。
     * @param state 参数 状态；parameter state。
     * @param inputPayload 参数 输入负载；parameter input payload。
     * @param resultPayload 参数 结果负载；parameter result payload。
     * @param errorPayload 参数 错误负载；parameter error payload。
     * @param workerOwner 参数 工作持有者；parameter worker owner。
     * @param leaseUntil 参数 租约到期；parameter lease until。
     * @param executionDeadline 参数 执行截止时间；parameter execution deadline。
     * @param expiresAt 参数 过期时间；parameter expires at。
     * @param attemptCount 参数 尝试次数；parameter attempt count。
     * @param maxAttempts 参数 最大尝试次数；parameter max attempts。
     * @param revision 参数 revision；parameter revision。
     * @param createdAt 参数 创建时间；parameter created at。
     * @param updatedAt 参数 更新时间；parameter updated at。
     * @return 返回已完成校验与防御性复制的任务载体；returns the validated, defensively copied task carrier.
     */
    public static McpTaskBO normalized(
            String id,
            String principalFingerprint,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String requestDigest,
            String state,
            Map<String, Object> inputPayload,
            Map<String, Object> resultPayload,
            Map<String, Object> errorPayload,
            String workerOwner,
            Instant leaseUntil,
            Instant executionDeadline,
            Instant expiresAt,
            int attemptCount,
            int maxAttempts,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {
        String checkedId = McpJdbcJson.required(id, "id");
        String checkedPrincipalFingerprint = McpJdbcJson.required(
                principalFingerprint,
                "principalFingerprint"
        );
        String checkedSubjectId =
                McpJdbcJson.required(subjectId, "subjectId");
        String checkedTenantId = McpJdbcJson.required(tenantId, "tenantId");
        String checkedClientId = McpJdbcJson.required(clientId, "clientId");
        String checkedServerCode =
                McpJdbcJson.required(serverCode, "serverCode");
        String checkedToolName =
                McpJdbcJson.required(toolName, "toolName");
        String checkedRequestDigest = digest(requestDigest, "requestDigest");
        String checkedState = JdbcMcpTaskRepository.state(state);
        Map<String, Object> checkedInputPayload = copy(inputPayload);
        Map<String, Object> checkedResultPayload = copy(resultPayload);
        Map<String, Object> checkedErrorPayload = copy(errorPayload);
        String checkedWorkerOwner = optional(workerOwner);
        Instant checkedExecutionDeadline = Objects.requireNonNull(
                executionDeadline,
                "executionDeadline"
        );
        Instant checkedExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt");
        Instant checkedCreatedAt =
                Objects.requireNonNull(createdAt, "createdAt");
        Instant checkedUpdatedAt =
                Objects.requireNonNull(updatedAt, "updatedAt");
        if ((checkedWorkerOwner == null) != (leaseUntil == null)) {
            throw new IllegalArgumentException(
                    "workerOwner and leaseUntil must be set together"
            );
        }
        if (!checkedExecutionDeadline.isAfter(checkedCreatedAt)
                || !checkedExpiresAt.isAfter(checkedCreatedAt)) {
            throw new IllegalArgumentException(
                    "task deadlines must be after createdAt"
            );
        }
        if (attemptCount < 0 || maxAttempts <= 0
                || attemptCount > maxAttempts || revision < 0) {
            throw new IllegalArgumentException(
                    "task attempts and revision are invalid"
            );
        }
        return new McpTaskBO(
                checkedId,
                checkedPrincipalFingerprint,
                checkedSubjectId,
                checkedTenantId,
                checkedClientId,
                checkedServerCode,
                checkedToolName,
                checkedRequestDigest,
                checkedState,
                checkedInputPayload,
                checkedResultPayload,
                checkedErrorPayload,
                checkedWorkerOwner,
                leaseUntil,
                checkedExecutionDeadline,
                checkedExpiresAt,
                attemptCount,
                maxAttempts,
                revision,
                checkedCreatedAt,
                checkedUpdatedAt
        );
    }

    /**
     * 中文说明：对可选负载做防御性复制，null 透传，非 null 时转为不可变视图；与原持久化载体的复制语义一致。
     * English summary: Defensively copies an optional payload, passing null through and freezing non-null values into an immutable view; matches the legacy carrier 's copy semantics.
     * @param value 参数 值；parameter value。
     * @return 返回复制后的负载或 null；returns the copied payload or null.
     */
    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null ? null : Map.copyOf(value);
    }

    /**
     * 中文说明：将空白字符串归一为 null，否则去除首尾空白；与原持久化载体的 optional 语义一致。
     * English summary: Normalizes a blank string to null, otherwise trims it; matches the legacy carrier 's optional semantics.
     * @param value 参数 值；parameter value。
     * @return 返回归一后的字符串或 null；returns the normalized string or null.
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 中文说明：校验摘要字段必填且长度恰为 64；与原持久化载体的 digest 语义一致。
     * English summary: Requires the digest field to be present and exactly 64 characters long; matches the legacy carrier 's digest semantics.
     * @param value 参数 值；parameter value。
     * @param field 参数 字段名；parameter field。
     * @return 返回校验通过的摘要；returns the validated digest.
     */
    private static String digest(String value, String field) {
        String digest = McpJdbcJson.required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }
}
