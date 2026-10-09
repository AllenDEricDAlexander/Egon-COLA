package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
public class OAuthPendingConsentBO {

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Size(min = 32, max = 512)
    private String clientState;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Size(max = 256)
    @Pattern(regexp = "[A-Za-z0-9_-]{32,256}={0,2}")
    private String consentState;

    @NotEmpty
    @Size(max = 64)
    private List<@NotBlank @Size(max = 128) String> requestedScopes;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String sessionBindingDigest;

    @NotNull
    private Instant issuedAt;

    @NotNull
    private Instant expiresAt;

    @JsonIgnore
    @AssertTrue
    public boolean isBoundedLifetime() {
        return issuedAt != null && expiresAt != null && expiresAt.isAfter(issuedAt)
                && Duration.between(issuedAt, expiresAt).compareTo(Duration.ofSeconds(300)) <= 0;
    }
}
