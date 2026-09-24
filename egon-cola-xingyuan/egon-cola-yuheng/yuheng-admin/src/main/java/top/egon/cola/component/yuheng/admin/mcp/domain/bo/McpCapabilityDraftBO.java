package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpCapabilityKindEnum;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpCapabilityDraftBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpCapabilityDraftBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpCapabilityDraftBO {

    private String gatewayGroupId;
    private Map<McpCapabilityKindEnum, List<McpCapabilityRecordBO>> capabilities;

    /**
     * 中文说明：按草稿契约建立不可变视图的业务工厂；Lombok 的规范构造器不承担该校验，因此写入边界必须调用本方法。
     * English summary: Business factory applying the draft's immutability contract; Lombok's canonical constructor cannot enforce it, so write boundaries must call this factory.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @return 返回已完成校验与防御性复制的草稿载体；returns the validated, defensively copied draft carrier.
     */
    public static McpCapabilityDraftBO normalized(
            String gatewayGroupId,
            Map<McpCapabilityKindEnum, List<McpCapabilityRecordBO>> capabilities
    ) {
        String checkedGroupId = required(gatewayGroupId, "gatewayGroupId");
        EnumMap<McpCapabilityKindEnum, List<McpCapabilityRecordBO>> copy =
                new EnumMap<>(McpCapabilityKindEnum.class);
        capabilities.forEach((kind, drafts) -> copy.put(kind, List.copyOf(drafts)));
        return new McpCapabilityDraftBO(checkedGroupId, Map.copyOf(copy));
    }

    /**
     * 中文说明：读取指定能力类别下的草稿集合，缺类别时返回空集合。
     * English summary: Reads the draft collection for one capability kind, returning an empty list when the kind is absent.
     * @param kind 参数 能力类别；parameter capability kind。
     * @return 返回该类别下的草稿集合；returns the drafts registered for that kind.
     */
    public List<McpCapabilityRecordBO> capabilities(McpCapabilityKindEnum kind) {
        return capabilities.getOrDefault(kind, List.of());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
