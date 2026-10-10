package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Exact browser origins for the existing tenant OAuth resources. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties("egon.tianquan-shoubing.oauth")
public class OAuthCorsProperties {
    @NotNull
    @Size(max = 64)
    @Builder.Default
    private List<@NotBlank @Size(max = 2048) String> allowedOrigins = List.of();

    public OAuthCorsProperties setAllowedOrigins(List<String> origins) {
        this.allowedOrigins = origins == null ? null : List.copyOf(origins);
        return this;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins == null ? null : List.copyOf(allowedOrigins);
    }

    @AssertTrue(message = "OAuth origins must be complete HTTPS origins or explicit IPv4 loopback development origins")
    public boolean isOriginShapeValid() {
        if (allowedOrigins == null) { return false; }
        return allowedOrigins.stream().allMatch(OAuthCorsProperties::validOrigin);
    }

    private static boolean validOrigin(String value) {
        if (value == null || value.length() > 2048 || !value.matches("[\\x21-\\x7e]+")) { return false; }
        try {
            URI origin = URI.create(value);
            return origin.isAbsolute() && origin.getHost() != null && origin.getUserInfo() == null
                    && origin.getQuery() == null && origin.getFragment() == null
                    && (origin.getRawPath() == null || origin.getRawPath().isEmpty())
                    && (origin.getPort() == -1 || origin.getPort() >= 1 && origin.getPort() <= 65535)
                    && ("https".equals(origin.getScheme()) || "http".equals(origin.getScheme())
                    && "127.0.0.1".equals(origin.getHost()) && origin.getPort() > 0);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
