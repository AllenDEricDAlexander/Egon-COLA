package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthAuthorizationAttributesBO {

    @Min(1)
    @Max(2)
    @Builder.Default
    private int schemaVersion = 1;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 512)
    private String nonce;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String codeChallenge;

    @Pattern(regexp = "S256")
    @Size(max = 16)
    private String codeChallengeMethod;

    private URI redirectUri;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String stateDigest;

    @NotNull
    @Valid
    private OAuthSecuritySnapshotBO securitySnapshot;

    @Size(max = 64)
    private String acr;

    private Instant authTime;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Valid
    private OAuthPendingConsentBO pendingConsent;

}
