package top.egon.cola.component.yuheng.edge.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Component("edgeOAuthProperties")
@ConfigurationProperties(prefix = "egon.yuheng.edge")
public class EdgeOAuthProperties {
    private Upstreams upstreams = new Upstreams();
    private Session session = new Session();
    private Logout logout = new Logout();
    private Refresh refresh = new Refresh();
    private URI publicOrigin;
    private String defaultRegistrationId;
    private List<String> allowedReturnPaths = new ArrayList<>(List.of("/"));

    @PostConstruct
    public void validate() {
        validateOrigin(upstreams.getAuthorizationServer(), "upstreams.authorization-server");
        validateOrigin(upstreams.getBizGateway(), "upstreams.biz-gateway");
        validateOrigin(upstreams.getMcpGateway(), "upstreams.mcp-gateway");
        validateOrigin(publicOrigin, "public-origin");
        if (publicOrigin.getPath() != null && !publicOrigin.getPath().isEmpty()) {
            throw new IllegalStateException("Edge public-origin must not contain a path");
        }
        if (blank(defaultRegistrationId)) {
            throw new IllegalStateException("Edge default OAuth registration id is required");
        }

        if (session.getAbsoluteTimeout() == null
                || session.getAbsoluteTimeout().isNegative()
                || session.getAbsoluteTimeout().isZero()
                || session.getAbsoluteTimeout().compareTo(Duration.ofHours(8)) > 0) {
            throw new IllegalStateException("Edge absolute session timeout must be within 0..8h");
        }
        if (!positive(session.getIdleTimeout())
                || session.getIdleTimeout().compareTo(session.getAbsoluteTimeout()) > 0) {
            throw new IllegalStateException("Edge session idle timeout must be positive and no longer than absolute timeout");
        }
        if (session.getMaxAuthorizedClients() < 1 || session.getMaxAuthorizedClients() > 1_000) {
            throw new IllegalStateException("Edge session AuthorizedClient limit must be within 1..1000");
        }
        if (session.getEncryption() == null
                || blank(session.getEncryption().getKeyId())
                || !session.getEncryption().getKeyId().matches("[A-Za-z0-9._-]{1,64}")
                || blank(session.getEncryption().getKeyFile())) {
            throw new IllegalStateException("Edge session encryption key id and file are required");
        }
        if (logout == null
                || blank(logout.getStream())
                || blank(logout.getConsumerGroup())
                || logout.getMaxPending() < 1
                || logout.getMaxPending() > 10_000
                || !boundedPositive(logout.getPollInterval(), Duration.ofMinutes(1))
                || !boundedPositive(logout.getClaimIdle(), Duration.ofMinutes(10))
                || !boundedPositive(logout.getTimeout(), Duration.ofSeconds(30))) {
            throw new IllegalStateException("Edge logout stream settings are invalid");
        }
        if (refresh == null
                || !boundedPositive(refresh.getLockWait(), Duration.ofSeconds(30))
                || !boundedPositive(refresh.getLockLease(), Duration.ofSeconds(60))
                || !boundedPositive(refresh.getTokenTimeout(), Duration.ofSeconds(30))
                || refresh.getLockLease().compareTo(refresh.getTokenTimeout()) <= 0) {
            throw new IllegalStateException("Edge refresh lock and token timeout settings are invalid");
        }
        if (allowedReturnPaths == null || allowedReturnPaths.isEmpty()
                || allowedReturnPaths.stream().anyMatch(path -> !safeReturnPath(path))) {
            throw new IllegalStateException("Edge allowed return paths must be safe local paths");
        }
    }

    @Override
    public String toString() {
        return "EdgeOAuthProperties[publicOrigin=" + publicOrigin
                + ", defaultRegistrationId=" + defaultRegistrationId
                + ", upstreams=<configured>, session=<configured>, logout=<configured>, refresh=<configured>]";
    }

    private static void validateOrigin(URI origin, String property) {
        if (origin == null
                || !"https".equalsIgnoreCase(origin.getScheme())
                || blank(origin.getHost())
                || origin.getUserInfo() != null
                || origin.getQuery() != null
                || origin.getFragment() != null) {
            throw new IllegalStateException("Invalid Edge origin property: " + property);
        }
    }

    private static boolean safeReturnPath(String path) {
        return path != null
                && path.startsWith("/")
                && !path.startsWith("//")
                && !path.contains("\\")
                && path.indexOf('\r') < 0
                && path.indexOf('\n') < 0;
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    private static boolean boundedPositive(Duration duration, Duration maximum) {
        return positive(duration) && duration.compareTo(maximum) <= 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Upstreams {
        private URI authorizationServer;
        private URI bizGateway;
        private URI mcpGateway;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Session {
        private Duration idleTimeout = Duration.ofMinutes(30);
        private Duration absoluteTimeout = Duration.ofHours(8);
        private int maxAuthorizedClients = 100;
        private Encryption encryption = new Encryption();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Encryption {
        private String keyId;
        private String keyFile;

        @Override
        public String toString() {
            return "Encryption[keyId=" + keyId + ", keyFile=<redacted>]";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Logout {
        private String stream = "egon:bff:logout-revocation:v1";
        private String consumerGroup = "bff-revocation-v1";
        private int maxPending = 10_000;
        private Duration pollInterval = Duration.ofSeconds(5);
        private Duration claimIdle = Duration.ofSeconds(30);
        private Duration timeout = Duration.ofSeconds(5);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Refresh {
        private Duration lockWait = Duration.ofSeconds(5);
        private Duration lockLease = Duration.ofSeconds(15);
        private Duration tokenTimeout = Duration.ofSeconds(5);
    }
}
