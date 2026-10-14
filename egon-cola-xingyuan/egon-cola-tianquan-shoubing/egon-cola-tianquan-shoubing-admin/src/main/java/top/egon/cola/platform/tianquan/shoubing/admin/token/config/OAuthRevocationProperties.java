package top.egon.cola.platform.tianquan.shoubing.admin.token.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@Qualifier("oauthRevocationProperties")
@ConfigurationProperties(prefix = "egon.cola.platform.tianquan.shoubing.revocation")
public class OAuthRevocationProperties {

    @NotNull
    private URI primaryEndpoint;

    @Builder.Default
    @NotBlank
    @Pattern(regexp = "STRICT_SHARED")
    private String mode = "STRICT_SHARED";

    @Builder.Default
    @NotNull
    private Duration timeout = Duration.ofMillis(500);

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
    private String epoch;

    public void validateWith(RedissonClient redisson) {
        Objects.requireNonNull(redisson, "redisson");
        validateValues();
        Config config = Objects.requireNonNull(redisson.getConfig(), "redissonConfig");
        if (config.isClusterConfig() || config.isSentinelConfig()) {
            throw new IllegalStateException("OAUTH_REVOCATION_PRIMARY_TOPOLOGY_INVALID");
        }
        SingleServerConfig singleServer = config.useSingleServer();
        String configuredAddress = singleServer == null ? null : singleServer.getAddress();
        if (!primaryEndpoint.toASCIIString().equals(configuredAddress)) {
            throw new IllegalStateException("OAUTH_REVOCATION_PRIMARY_ENDPOINT_MISMATCH");
        }
    }

    public void validateValues() {
        boolean loopbackPlaintext = primaryEndpoint != null
                && "redis".equals(primaryEndpoint.getScheme())
                && primaryEndpoint.getHost() != null
                && Set.of("127.0.0.1", "localhost", "::1", "[::1]").contains(primaryEndpoint.getHost());
        if (primaryEndpoint == null || !primaryEndpoint.isAbsolute()
                || !("rediss".equalsIgnoreCase(primaryEndpoint.getScheme()) || loopbackPlaintext)
                || primaryEndpoint.getHost() == null || primaryEndpoint.getUserInfo() != null
                || primaryEndpoint.getQuery() != null || primaryEndpoint.getFragment() != null
                || (primaryEndpoint.getRawPath() != null && !primaryEndpoint.getRawPath().isEmpty())
                || !"STRICT_SHARED".equals(mode)
                || timeout == null || timeout.isZero() || timeout.isNegative()
                || timeout.compareTo(Duration.ofMillis(500)) > 0
                || epoch == null || !epoch.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new IllegalStateException("OAUTH_REVOCATION_PROPERTIES_INVALID");
        }
        primaryEndpoint = primaryEndpoint.normalize();
    }
}
