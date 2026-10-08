package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Safe OAuth client management view. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthClientVO {

    @NotBlank
    @Size(max = 128)
    private String clientId;
    @Size(max = 128)
    private String appId;
    @NotBlank
    @Size(max = 200)
    private String clientName;
    @NotBlank
    @Size(max = 16)
    private String clientType;
    @NotBlank
    @Size(max = 16)
    private String purpose;
    @NotBlank
    @Size(max = 16)
    private String status;
    private boolean pkceRequired;
    private int accessTokenTtlSeconds;
    private int refreshTokenTtlSeconds;
    @NotNull
    @Size(max = 16)
    private List<String> redirectUris;
    @NotNull
    @Size(max = 64)
    private List<String> resourceUris;
    @NotNull
    @Size(max = 64)
    private List<String> allowedScopes;
    @PositiveOrZero
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
    @Size(max = 8)
    private String secretHint;
    @Size(max = 16)
    private String secretStatus;
    @NotBlank
    @Size(max = 2048)
    private String issuer;
}
