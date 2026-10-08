package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Safe Client Resource Grant administration view. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientResourceGrantVO {

    @NotBlank
    @Size(max = 128)
    private String clientId;
    @NotBlank
    @Size(max = 128)
    private String resourceServerId;
    @NotBlank
    @Size(max = 32)
    private String grantType;
    @Size(max = 32)
    private String tenantId;
    @Size(max = 64)
    private Set<@NotBlank @Size(max = 256) String> allowedScopes;
    @NotBlank
    @Size(max = 16)
    private String status;
    @PositiveOrZero
    private long version;
}
