package top.egon.cola.component.yuheng.admin.catalog.domain.bo;

import java.time.Instant;
import java.util.Map;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayOperationBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayOperationBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayOperationBO {

    private String id;
    private String applicationId;
    private String interfaceGroupId;
    private String operationKey;
    private String protocol;
    private String methodIdentity;
    private boolean externalAccessible;
    private Map<String, Object> providerServiceIdentity;
    private String sourceType;
    private String lifecycleStatus;
    private String currentDefinitionId;
    private long revision;
    private Instant createdAt;
    private Instant updatedAt;
}
