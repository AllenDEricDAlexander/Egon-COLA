package top.egon.cola.component.tianshu.model.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 配置发布与服务注册Topic使用的tenant外层信封。
 * / Tenant envelope for configuration and registry topic payloads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcTenantMessageDTO<T> {

    /** 消息所属的可信存储tenant。 / Trusted storage tenant that owns this topic message. */
    @NotNull
    @Positive
    private Long tenantId;

    /** 原业务payload；在tenant信封内保持不变。 / Existing business payload, kept unchanged in the tenant envelope. */
    @NotNull
    @Valid
    @ToString.Exclude
    private T payload;

    /** 判断信封是否属于本地配置tenant。 / Returns whether the envelope belongs to the locally configured tenant. */
    public boolean belongsToTenant(Long expectedTenantId) {
        return expectedTenantId != null && expectedTenantId > 0
                && tenantId != null && tenantId.equals(expectedTenantId);
    }
}
