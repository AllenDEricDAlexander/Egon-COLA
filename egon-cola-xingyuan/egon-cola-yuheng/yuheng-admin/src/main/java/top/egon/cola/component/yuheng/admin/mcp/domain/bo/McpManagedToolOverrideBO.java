package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.util.Objects;
import java.util.Set;
import top.egon.cola.component.yuheng.admin.mcp.repository.jdbc.McpJdbcJson;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpManagedToolOverrideBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpManagedToolOverrideBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpManagedToolOverrideBO {

    private String toolId;
    private String gatewayGroupId;
    private String operationId;
    private String serverId;
    private Set<String> additionalPermissions;
    private String minimumRiskLevel;
    private Boolean enabled;
    private long revision;

    /**
     * 中文说明：按原持久化载体的紧凑构造器契约建立受管工具覆盖载体；Lombok 规范构造器不承担这些收紧式不变式，故写入边界必须经此工厂。
     * English summary: Builds a managed tool override carrier enforcing the tightening invariants of the legacy persistence carrier's compact constructor; Lombok's canonical constructor cannot, so write boundaries must go through this factory.
     *
     * 用法 / Usage: 由服务写入构造覆盖时调用；/ Call it when a service writes an override.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param additionalPermissions 参数 附加权限；parameter additional permissions。
     * @param minimumRiskLevel 参数 最低风险级别；parameter minimum risk level。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     * @return 返回已完成收紧校验与防御性复制的覆盖载体；returns the validated, defensively copied override carrier.
     */
    public static McpManagedToolOverrideBO normalized(
            String toolId,
            String gatewayGroupId,
            String operationId,
            String serverId,
            Set<String> additionalPermissions,
            String minimumRiskLevel,
            Boolean enabled,
            long revision
    ) {
        String checkedToolId = McpJdbcJson.required(toolId, "toolId");
        String checkedGatewayGroupId =
                McpJdbcJson.required(gatewayGroupId, "gatewayGroupId");
        String checkedOperationId =
                McpJdbcJson.required(operationId, "operationId");
        Set<String> checkedPermissions = Set.copyOf(Objects.requireNonNull(
                additionalPermissions,
                "additionalPermissions"
        ));
        if (Boolean.TRUE.equals(enabled)) {
            throw new IllegalArgumentException(
                    "managed Tool override cannot enable a Tool"
            );
        }
        if (serverId == null && checkedPermissions.isEmpty()
                && minimumRiskLevel == null && enabled == null) {
            throw new IllegalArgumentException(
                    "managed Tool override must tighten at least one field"
            );
        }
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
        return new McpManagedToolOverrideBO(
                checkedToolId,
                checkedGatewayGroupId,
                checkedOperationId,
                serverId,
                checkedPermissions,
                minimumRiskLevel,
                enabled,
                revision
        );
    }
}
