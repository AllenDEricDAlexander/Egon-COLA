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
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthResourceBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotBlank
    @Size(max = 128)
    private String resourceServerId;
    @NotNull
    private URI resourceUri;
    @NotBlank
    @Size(max = 128)
    private String bizCode;
    @NotBlank
    @Size(max = 128)
    private String appCode;
    @NotBlank
    @Size(max = 128)
    private String environment;
    @NotBlank
    @Size(max = 200)
    private String displayName;
    @NotBlank
    @Size(max = 128)
    private String managementClientId;
    @Positive
    private Long managementClientPk;
    @NotBlank
    @Size(max = 128)
    private String rbacApplicationCode;
    @NotBlank
    @Size(max = 256)
    private String entryPermissionCode;
    @NotNull
    private ResourceServerStatus status;
    @NotNull
    private Duration admissionTicketTtl;
    private Instant createdAt;
    private Instant updatedAt;
}
