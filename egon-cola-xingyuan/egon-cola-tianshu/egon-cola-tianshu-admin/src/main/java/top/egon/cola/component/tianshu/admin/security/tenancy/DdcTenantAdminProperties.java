package top.egon.cola.component.tianshu.admin.security.tenancy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(prefix = "egon.cola.component.tianshu.admin.tenancy")
public class DdcTenantAdminProperties {

    @Builder.Default
    @NotNull
    private List<@Valid DdcTenantBindingDTO> bindings = new ArrayList<>();

    @Builder.Default
    @NotNull
    private List<@Valid DdcCredentialTenantDTO> credentialBindings = new ArrayList<>();
}
