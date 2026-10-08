package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthTokenBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotNull
    @Positive
    private Long authorizationPk;
    @NotNull
    private OAuthTokenTypeEnum tokenType;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String tokenDigest;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String tokenValue;
    @Pattern(regexp = "[0-9]{1,64}")
    private String jti;
    @NotNull
    private Instant issuedAt;
    @NotNull
    private Instant expiresAt;
    private Instant consumedAt;
    private Instant revokedAt;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull
    @Valid
    private OAuthTokenMetadataBO metadata;
}
