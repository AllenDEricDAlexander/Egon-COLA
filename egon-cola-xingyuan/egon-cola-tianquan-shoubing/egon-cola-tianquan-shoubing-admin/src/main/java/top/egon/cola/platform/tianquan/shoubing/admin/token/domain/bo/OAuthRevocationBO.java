package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthRevocationBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotNull
    private OAuthSecurityTargetEnum targetType;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Size(max = 128)
    private String targetKey;
    @NotBlank
    @Size(max = 128)
    private String reasonCode;
    @NotNull
    private Instant revokedAt;
    private Instant expiresAt;
    @Min(0)
    private long revision;
    @NotBlank
    @Size(max = 16)
    private String projectionStatus;
}
