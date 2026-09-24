package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.util.Map;
import java.util.Objects;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpCapabilityKindEnum;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpCapabilityRecordBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpCapabilityRecordBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpCapabilityRecordBO {

    private McpCapabilityKindEnum kind;
    private String id;
    private String gatewayGroupId;
    private String serverId;
    private String name;
    private Map<String, Object> content;
    private boolean enabled;
    private long revision;

    /**
     * 中文说明：按原持久化载体的紧凑构造器契约建立能力记录载体；Lombok 规范构造器不承担这些不变式，故读写边界必须经此工厂。
     * English summary: Builds a capability record carrier enforcing the invariants of the legacy persistence carrier's compact constructor; Lombok's canonical constructor cannot, so read/write boundaries must go through this factory.
     *
     * 用法 / Usage: 由 Jdbc 载入与服务写入构造新记录时调用；/ Call it when the Jdbc load path or a service writes a new record.
     * @return 返回已完成必填校验、防御性复制与非负修订校验的记录载体；returns the validated, defensively copied record carrier.
     */
    public static McpCapabilityRecordBO normalized(
            McpCapabilityKindEnum kind,
            String id,
            String gatewayGroupId,
            String serverId,
            String name,
            Map<String, Object> content,
            boolean enabled,
            long revision
    ) {
        McpCapabilityKindEnum checkedKind = Objects.requireNonNull(kind, "kind");
        String checkedId = required(id, "id");
        String checkedGatewayGroupId =
                required(gatewayGroupId, "gatewayGroupId");
        String checkedServerId = required(serverId, "serverId");
        String checkedName = required(name, "name");
        Map<String, Object> checkedContent =
                Map.copyOf(Objects.requireNonNull(content, "content"));
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
        return new McpCapabilityRecordBO(
                checkedKind,
                checkedId,
                checkedGatewayGroupId,
                checkedServerId,
                checkedName,
                checkedContent,
                enabled,
                revision
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
