package top.egon.cola.component.gateway.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("egon.cola.component.gateway.reporting")
public class GatewayReportingProperties {

    private boolean enabled;

    private String adminBaseUrl;

    private String applicationCode;

    private String applicationName;

    private String env;

    private String namespace = "default";

    private String artifactVersion;

    private String buildId;

    private List<String> declaredHosts = new ArrayList<>();

    private boolean failFast;

    private String accessKey;

    private String secretKey;

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);

    private int maxAttempts = 5;

    private Duration reconcileInterval = Duration.ofMinutes(5);

    private String stateFile =
            "data/gateway-definition-report.state";

    public void validate() {
        if (!enabled) {
            return;
        }
        required(adminBaseUrl, "adminBaseUrl");
        required(applicationCode, "applicationCode");
        required(applicationName, "applicationName");
        required(env, "env");
        required(namespace, "namespace");
        required(artifactVersion, "artifactVersion");
        required(buildId, "buildId");
        required(accessKey, "accessKey");
        required(secretKey, "secretKey");
        if (maxAttempts < 1 || maxAttempts > 20) {
            throw new IllegalArgumentException(
                    "maxAttempts must be between 1 and 20"
            );
        }
        if (reconcileInterval == null
                || reconcileInterval.compareTo(
                Duration.ofMillis(10)
        ) < 0
                || reconcileInterval.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException(
                    "reconcileInterval must be between PT0.01S and P1D"
            );
        }
        required(stateFile, "stateFile");
    }

    private void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "gateway reporting " + field + " is required"
            );
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminBaseUrl() {
        return adminBaseUrl;
    }

    public void setAdminBaseUrl(String adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public List<String> getDeclaredHosts() {
        return List.copyOf(declaredHosts);
    }

    public void setDeclaredHosts(List<String> declaredHosts) {
        this.declaredHosts = declaredHosts == null
                ? new ArrayList<>()
                : new ArrayList<>(declaredHosts);
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getReconcileInterval() {
        return reconcileInterval;
    }

    public void setReconcileInterval(Duration reconcileInterval) {
        this.reconcileInterval = reconcileInterval;
    }

    public String getStateFile() {
        return stateFile;
    }

    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }
}
