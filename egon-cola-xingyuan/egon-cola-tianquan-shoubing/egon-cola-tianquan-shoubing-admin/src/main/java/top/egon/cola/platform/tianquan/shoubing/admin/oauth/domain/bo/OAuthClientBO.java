package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

import java.util.List;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthClientBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotBlank
    @Size(max = 128)
    private String clientId;
    @Size(max = 128)
    private String appId;
    @NotBlank
    @Size(max = 200)
    private String clientName;
    @NotNull
    private OAuthClient.ClientType clientType;
    @NotNull
    private OAuthClientPurposeEnum purpose;
    @NotNull
    private OAuthClient.Status status;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 512)
    private String secretHash;
    @JsonIgnore
    @ToString.Exclude
    @Size(max = 8)
    private String secretHint;
    @NotNull
    @PositiveOrZero
    private Long credentialVersion;
    @NotNull
    @Size(max = 16)
    private List<@NotBlank @Size(max = 2048) String> redirectUris;
    @NotNull
    @Size(max = 64)
    private List<@NotBlank @Size(max = 2048) String> resourceUris;
    @NotNull
    @Size(max = 64)
    private List<@NotBlank @Size(max = 128) String> allowedScopes;
    @NotNull
    @Min(300)
    @Max(600)
    private Integer accessTtlSeconds;
    @NotNull
    @Min(0)
    @Max(604800)
    private Integer refreshTtlSeconds;
    private Instant createdAt;
    private Instant updatedAt;
}
