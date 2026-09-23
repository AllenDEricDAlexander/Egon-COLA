package top.egon.cola.component.yuheng.admin.release.domain.bo;

import java.time.Instant;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayReleaseTargetBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayReleaseTargetBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayReleaseTargetBO {

    private String instanceId;
    private String leaseId;
    private String status;
    private Long appliedVersion;
    private String appliedArtifactSha256;
    private String errorCode;
    private Instant observedAt;
    private GatewayEngineRoleEnum engineRole;
}
