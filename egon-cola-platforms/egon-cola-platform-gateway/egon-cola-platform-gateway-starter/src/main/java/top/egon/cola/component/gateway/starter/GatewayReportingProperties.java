package top.egon.cola.component.gateway.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for reporting discovered Gateway interface
 * definitions to Gateway Admin.
 */
@ConfigurationProperties("egon.cola.component.gateway.reporting")
public class GatewayReportingProperties {

    /** Whether interface definition reporting is enabled. */
    private boolean enabled;

    /** Base URL of the Gateway Admin reporting endpoint. */
    private String adminBaseUrl;

    /** Stable code identifying the reporting application. */
    private String applicationCode;

    /** Business code that owns the reporting application. */
    private String bizCode;

    /** Human-readable name of the reporting application. */
    private String applicationName;

    /** Environment in which the reporting application is running. */
    private String env;

    /** Namespace used to scope the reported definitions. */
    private String namespace = "default";

    /** Version of the artifact that contributes the definitions. */
    private String artifactVersion;

    /** Identifier of the concrete application build. */
    private String buildId;

    /** Hosts explicitly declared by the application for HTTP exposure. */
    private List<String> declaredHosts = new ArrayList<>();

    /** Whether initial reporting failures should fail application startup. */
    private boolean failFast;

    /** Access key used to sign requests sent to Gateway Admin. */
    private String accessKey;

    /** Secret key used to sign requests sent to Gateway Admin. */
    private String secretKey;

    /** Maximum time allowed to establish the Admin HTTP connection. */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /** Maximum time allowed to read an Admin HTTP response. */
    private Duration readTimeout = Duration.ofSeconds(10);

    /** Maximum number of consecutive short-backoff reporting attempts. */
    private int maxAttempts = 5;

    /** Delay before reconciliation resumes after short retries are exhausted. */
    private Duration reconcileInterval = Duration.ofMinutes(5);

    /** Local file used to persist pending and acknowledged report state. */
    private String stateFile =
            "data/gateway-definition-report.state";

    /**
     * Validates all properties required when reporting is enabled.
     *
     * @throws IllegalArgumentException if a required property is missing or a
     *                                  retry setting is outside its supported
     *                                  range
     */
    public void validate() {
        if (!enabled) {
            return;
        }
        required(adminBaseUrl, "adminBaseUrl");
        required(applicationCode, "applicationCode");
        required(bizCode, "bizCode");
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

    /**
     * Requires a non-blank configuration value.
     *
     * @param value configured value
     * @param field property name used in validation errors
     * @throws IllegalArgumentException if {@code value} is null or blank
     */
    private void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "gateway reporting " + field + " is required"
            );
        }
    }

    /**
     * Returns whether interface definition reporting is enabled.
     *
     * @return {@code true} when reporting is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether interface definition reporting is enabled.
     *
     * @param enabled whether reporting is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the Gateway Admin base URL.
     *
     * @return Gateway Admin base URL
     */
    public String getAdminBaseUrl() {
        return adminBaseUrl;
    }

    /**
     * Sets the Gateway Admin base URL.
     *
     * @param adminBaseUrl Gateway Admin base URL
     */
    public void setAdminBaseUrl(String adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    /**
     * Returns the reporting application code.
     *
     * @return application code
     */
    public String getApplicationCode() {
        return applicationCode;
    }

    /**
     * Sets the reporting application code.
     *
     * @param applicationCode application code
     */
    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    /**
     * Returns the owning business code.
     *
     * @return business code
     */
    public String getBizCode() {
        return bizCode;
    }

    /**
     * Sets the owning business code.
     *
     * @param bizCode business code
     */
    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    /**
     * Returns the reporting application name.
     *
     * @return application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the reporting application name.
     *
     * @param applicationName application name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Returns the application environment.
     *
     * @return environment name
     */
    public String getEnv() {
        return env;
    }

    /**
     * Sets the application environment.
     *
     * @param env environment name
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * Returns the definition namespace.
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the definition namespace.
     *
     * @param namespace namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the reporting artifact version.
     *
     * @return artifact version
     */
    public String getArtifactVersion() {
        return artifactVersion;
    }

    /**
     * Sets the reporting artifact version.
     *
     * @param artifactVersion artifact version
     */
    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    /**
     * Returns the application build identifier.
     *
     * @return build identifier
     */
    public String getBuildId() {
        return buildId;
    }

    /**
     * Sets the application build identifier.
     *
     * @param buildId build identifier
     */
    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    /**
     * Returns an immutable snapshot of explicitly declared hosts.
     *
     * @return declared hosts
     */
    public List<String> getDeclaredHosts() {
        return List.copyOf(declaredHosts);
    }

    /**
     * Replaces the explicitly declared hosts with a defensive copy.
     *
     * @param declaredHosts declared hosts, or {@code null} to clear them
     */
    public void setDeclaredHosts(List<String> declaredHosts) {
        this.declaredHosts = declaredHosts == null
                ? new ArrayList<>()
                : new ArrayList<>(declaredHosts);
    }

    /**
     * Returns whether an initial reporting failure fails startup.
     *
     * @return {@code true} when fail-fast reporting is enabled
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether an initial reporting failure fails startup.
     *
     * @param failFast whether fail-fast reporting is enabled
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Returns the reporting access key.
     *
     * @return access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Sets the reporting access key.
     *
     * @param accessKey access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Returns the reporting secret key.
     *
     * @return secret key
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Sets the reporting secret key.
     *
     * @param secretKey secret key
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Returns the Admin connection timeout.
     *
     * @return connection timeout
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the Admin connection timeout.
     *
     * @param connectTimeout connection timeout
     */
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Returns the Admin response read timeout.
     *
     * @return read timeout
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the Admin response read timeout.
     *
     * @param readTimeout read timeout
     */
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Returns the maximum consecutive short-backoff attempts.
     *
     * @return maximum attempt count
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets the maximum consecutive short-backoff attempts.
     *
     * @param maxAttempts maximum attempt count
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Returns the long reconciliation interval.
     *
     * @return reconciliation interval
     */
    public Duration getReconcileInterval() {
        return reconcileInterval;
    }

    /**
     * Sets the long reconciliation interval.
     *
     * @param reconcileInterval reconciliation interval
     */
    public void setReconcileInterval(Duration reconcileInterval) {
        this.reconcileInterval = reconcileInterval;
    }

    /**
     * Returns the local reporting state file path.
     *
     * @return state file path
     */
    public String getStateFile() {
        return stateFile;
    }

    /**
     * Sets the local reporting state file path.
     *
     * @param stateFile state file path
     */
    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }
}
