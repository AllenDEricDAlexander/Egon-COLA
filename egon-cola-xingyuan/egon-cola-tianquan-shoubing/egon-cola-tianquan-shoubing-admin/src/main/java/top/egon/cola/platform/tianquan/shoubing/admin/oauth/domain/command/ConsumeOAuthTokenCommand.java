package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
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
public class ConsumeOAuthTokenCommand {

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String tokenDigest;

    @NotNull
    @Positive
    private Long clientPk;

    @NotNull
    private OAuthTokenTypeEnum tokenType;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String verifier;

    private URI redirectUri;

    @Size(max = 64)
    private Set<@NotBlank @Size(max = 128) String> scopes;

    private URI resource;
}
