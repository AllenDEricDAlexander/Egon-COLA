package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Resource Server administration view. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceServerVO {

    @NotBlank @Size(max = 128)
    private String resourceServerId;
    @NotBlank @Size(max = 2048)
    private String resourceUri;
    @NotBlank @Size(max = 128)
    private String bizCode;
    @NotBlank @Size(max = 128)
    private String appCode;
    @NotBlank @Size(max = 128)
    private String environment;
    @NotBlank @Size(max = 200)
    private String displayName;
    @NotBlank @Size(max = 128)
    private String managementClientId;
    @NotBlank @Size(max = 128)
    private String rbacApplicationCode;
    @NotBlank @Size(max = 256)
    private String entryPermissionCode;
    @NotBlank @Size(max = 16)
    private String status;
    @PositiveOrZero
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
}
