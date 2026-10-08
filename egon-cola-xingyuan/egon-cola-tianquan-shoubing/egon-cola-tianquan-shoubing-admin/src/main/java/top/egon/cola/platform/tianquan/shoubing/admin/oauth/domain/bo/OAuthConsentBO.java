package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

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
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthConsentBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotNull
    @Positive
    private Long clientPk;
    @NotBlank
    @Size(max = 128)
    private String subject;
    @NotNull
    @Size(max = 64)
    private List<@NotBlank @Size(max = 128) String> authorities;
    @NotNull
    private Instant grantedAt;
    @NotBlank
    @Pattern(regexp = "ACTIVE|REVOKED")
    private String status;
}
