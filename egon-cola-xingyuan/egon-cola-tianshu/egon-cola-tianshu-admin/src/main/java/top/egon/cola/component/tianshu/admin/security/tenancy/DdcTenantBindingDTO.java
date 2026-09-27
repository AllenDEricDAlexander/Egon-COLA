package top.egon.cola.component.tianshu.admin.security.tenancy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcTenantBindingDTO {

    @NotNull
    @Positive
    private Long tenantId;

    @NotBlank
    private String identityTenantId;

    private boolean enabled;
}
