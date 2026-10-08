package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthAuthorizationBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotNull
    @Positive
    private Long clientPk;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Size(max = 128)
    private String principalName;
    @NotBlank
    @Pattern(regexp = "USER|SERVICE")
    private String principalType;
    @NotBlank
    @Pattern(regexp = "authorization_code|client_credentials")
    @Size(max = 32)
    private String grantType;
    @NotNull
    @Size(max = 64)
    private List<@NotBlank @Size(max = 128) String> authorizedScopes;
    @NotNull
    @Size(max = 64)
    private List<@NotBlank @Size(max = 2048) String> audiences;
    @NotBlank
    @Pattern(regexp = "ACTIVE|REVOKED|EXPIRED")
    private String status;
    private Instant authTime;
    @Size(max = 64)
    private String acr;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull
    @Valid
    private OAuthAuthorizationAttributesBO attributes;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 4)
    private List<@Valid OAuthTokenBO> tokens;
    @NotNull
    private Instant absoluteExpiresAt;
    private Instant revokedAt;
}
