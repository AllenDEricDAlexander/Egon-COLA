package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.time.Instant;
import java.util.Objects;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpApprovalBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpApprovalBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpApprovalBO {

    private String id;
    private String tokenDigest;
    private String subjectId;
    private String tenantId;
    private String clientId;
    private String serverCode;
    private String toolName;
    private String argumentDigest;
    private Instant issuedAt;
    private Instant expiresAt;

    /**
     * 中文说明：按原持久化载体的紧凑构造器契约建立审批载体；Lombok 规范构造器不承担这些校验，故持久化读写边界必须经此工厂。
     * English summary: Builds an approval carrier enforcing the invariants of the legacy persistence carrier's compact constructor; Lombok's canonical constructor cannot, so persistence read/write boundaries must go through this factory.
     *
     * 用法 / Usage: 由审批仓储在签发与载入时调用；/ Call it from the approval repository when issuing or loading an approval.
     * @param id 参数 id；parameter id。
     * @param tokenDigest 参数 令牌摘要；parameter token digest。
     * @param subjectId 参数 主体Id；parameter subject id。
     * @param tenantId 参数 租户Id；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具名称；parameter tool name。
     * @param argumentDigest 参数 参数摘要；parameter argument digest。
     * @param issuedAt 参数 签发时间；parameter issued at。
     * @param expiresAt 参数 过期时间；parameter expires at。
     * @return 返回已完成校验的审批载体；returns the validated approval carrier.
     */
    public static McpApprovalBO normalized(
            String id,
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant issuedAt,
            Instant expiresAt
    ) {
        String checkedId = required(id, "id");
        String checkedTokenDigest = digest(tokenDigest, "tokenDigest");
        String checkedSubjectId =
                required(subjectId, "subjectId");
        String checkedTenantId = required(tenantId, "tenantId");
        String checkedClientId = required(clientId, "clientId");
        String checkedServerCode =
                required(serverCode, "serverCode");
        String checkedToolName =
                required(toolName, "toolName");
        String checkedArgumentDigest =
                digest(argumentDigest, "argumentDigest");
        Instant checkedIssuedAt =
                Objects.requireNonNull(issuedAt, "issuedAt");
        Instant checkedExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt");
        if (!checkedExpiresAt.isAfter(checkedIssuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
        return new McpApprovalBO(
                checkedId,
                checkedTokenDigest,
                checkedSubjectId,
                checkedTenantId,
                checkedClientId,
                checkedServerCode,
                checkedToolName,
                checkedArgumentDigest,
                checkedIssuedAt,
                checkedExpiresAt
        );
    }

    /**
     * 中文说明：校验摘要字段必填且长度恰为 64；与原持久化载体的 digest 语义一致。
     * English summary: Requires the digest field to be present and exactly 64 characters long; matches the legacy carrier 's digest semantics.
     * @param value 参数 值；parameter value。
     * @param field 参数 字段名；parameter field。
     * @return 返回校验通过的摘要；returns the validated digest.
     */
    private static String digest(String value, String field) {
        String digest = required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
