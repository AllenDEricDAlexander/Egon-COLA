package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

/** OAuth client update input. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class UpdateOAuthClientDTO {

    @NotBlank
    @Size(max = 200)
    private String clientName;

    @NotNull
    private OAuthClient.Status status;

    @Min(300)
    @Max(600)
    private int accessTokenTtlSeconds;

    @Min(0)
    @Max(604800)
    private int refreshTokenTtlSeconds;

    @PositiveOrZero
    private long expectedVersion;

    @Size(max = 64)
    private List<@NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String> allowedScopes;
}
