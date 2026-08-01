package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("egon.rbac3.gateway-status")
public class Rbac3GatewayStatusProperties {

    private URI adminBaseUrl;
    private String gatewayGroupId;
    private String releaseId;
    private Path oauthTokenFile;
    private Duration timeout = Duration.ofSeconds(3);

    public URI requireAdminBaseUrl() {
        return required(adminBaseUrl, "adminBaseUrl");
    }

    public String requireGatewayGroupId() {
        return required(gatewayGroupId, "gatewayGroupId");
    }

    public String requireReleaseId() {
        return required(releaseId, "releaseId");
    }

    public Path requireOauthTokenFile() {
        return required(oauthTokenFile, "oauthTokenFile");
    }

    public Duration requireTimeout() {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "egon.rbac3.gateway-status.timeout must be positive");
        }
        return timeout;
    }

    public void setAdminBaseUrl(URI adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    public void setGatewayGroupId(String gatewayGroupId) {
        this.gatewayGroupId = gatewayGroupId;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    public void setOauthTokenFile(Path oauthTokenFile) {
        this.oauthTokenFile = oauthTokenFile;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "egon.rbac3.gateway-status." + field + " is required");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "egon.rbac3.gateway-status." + field + " is required");
        }
        return value.trim();
    }
}
