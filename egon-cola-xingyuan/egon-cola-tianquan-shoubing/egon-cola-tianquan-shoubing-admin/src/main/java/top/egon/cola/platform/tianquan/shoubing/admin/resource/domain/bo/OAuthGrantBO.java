package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthGrantBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotNull
    @Positive
    private Long clientPk;
    @NotBlank
    @Size(max = 128)
    private String resourceServerId;
    @NotNull
    private ResourceGrantType grantType;
    @NotNull
    @Size(max = 64)
    private List<@NotBlank @Size(max = 256) String> allowedScopes;
    @NotNull
    private ClientResourceGrant.Status status;
    @NotNull
    @PositiveOrZero
    private Long resourceVersion;
}
