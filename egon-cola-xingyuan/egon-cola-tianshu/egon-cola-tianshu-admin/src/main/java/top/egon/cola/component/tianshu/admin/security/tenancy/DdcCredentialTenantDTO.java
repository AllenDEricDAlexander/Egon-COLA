package top.egon.cola.component.tianshu.admin.security.tenancy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcCredentialTenantDTO {

    @NotBlank
    private String credentialId;

    @NotNull
    @Positive
    private Long tenantId;

    @Builder.Default
    @NotNull
    private List<@NotBlank String> namespacePatterns = new ArrayList<>();
}
