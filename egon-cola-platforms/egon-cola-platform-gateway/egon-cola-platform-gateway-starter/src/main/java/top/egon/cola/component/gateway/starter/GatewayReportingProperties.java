package top.egon.cola.component.gateway.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for reporting discovered Gateway interface
 * definitions to Gateway Admin.
 *
 * <p>中文：配置向 Gateway Admin 上报已发现接口定义所需的全部属性。
 */
@ConfigurationProperties("egon.cola.component.gateway.reporting")
public class GatewayReportingProperties {

    /** Whether interface definition reporting is enabled. 是否启用接口定义上报。 */
    private boolean enabled;

    /**
     * Base URL of the Gateway Admin reporting endpoint.
     * Gateway Admin 上报端点的基础 URL。
     */
    private String adminBaseUrl;

    /** Stable code identifying the reporting application. 标识上报应用的稳定编码。 */
    private String applicationCode;

    /** Business code that owns the reporting application. 上报应用所属的业务编码。 */
    private String bizCode;

    /** Human-readable name of the reporting application. 上报应用的可读名称。 */
    private String applicationName;

    /**
     * Environment in which the reporting application is running.
     * 上报应用运行所在的环境。
     */
    private String env;

    /**
     * Namespace used to scope the reported definitions.
     * 用于限定接口定义范围的命名空间。
     */
    private String namespace = "default";

    /**
     * Version of the artifact that contributes the definitions.
     * 提供接口定义的构件版本。
     */
    private String artifactVersion;

    /** Identifier of the concrete application build. 具体应用构建的标识。 */
    private String buildId;

    /**
     * Hosts explicitly declared by the application for HTTP exposure.
     * 应用显式声明的 HTTP 暴露主机。
     */
    private List<String> declaredHosts = new ArrayList<>();

    /**
     * Whether initial reporting failures should fail application startup.
     * 初次上报失败时是否阻止应用启动。
     */
    private boolean failFast;

    /**
     * Access key used to sign requests sent to Gateway Admin.
     * 向 Gateway Admin 发送签名请求使用的访问密钥。
     */
    private String accessKey;

    /**
     * Secret key used to sign requests sent to Gateway Admin.
     * 向 Gateway Admin 发送签名请求使用的秘密密钥。
     */
    private String secretKey;

    /**
     * Maximum time allowed to establish the Admin HTTP connection.
     * 建立 Admin HTTP 连接允许的最长时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /**
     * Maximum time allowed to read an Admin HTTP response.
     * 读取 Admin HTTP 响应允许的最长时间。
     */
    private Duration readTimeout = Duration.ofSeconds(10);

    /**
     * Maximum number of consecutive short-backoff reporting attempts.
     * 连续短退避上报的最大尝试次数。
     */
    private int maxAttempts = 5;

    /**
     * Delay before reconciliation resumes after short retries are exhausted.
     * 短重试耗尽后恢复协调前的等待时间。
     */
    private Duration reconcileInterval = Duration.ofMinutes(5);

    /**
     * Local file used to persist pending and acknowledged report state.
     * 持久化待处理和已确认报告状态的本地文件。
     */
    private String stateFile =
            "data/gateway-definition-report.state";

    /**
     * Validates all properties required when reporting is enabled.
     * 中文：上报启用时校验所有必需配置，并检查重试参数范围。
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
     * 中文：要求配置值非空且不全为空白。
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
     * 中文：返回是否启用接口定义上报。
     *
     * @return {@code true} when reporting is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether interface definition reporting is enabled.
     * 中文：设置是否启用接口定义上报。
     *
     * @param enabled whether reporting is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the Gateway Admin base URL.
     * 中文：返回 Gateway Admin 基础 URL。
     *
     * @return Gateway Admin base URL
     */
    public String getAdminBaseUrl() {
        return adminBaseUrl;
    }

    /**
     * Sets the Gateway Admin base URL.
     * 中文：设置 Gateway Admin 基础 URL。
     *
     * @param adminBaseUrl Gateway Admin base URL
     */
    public void setAdminBaseUrl(String adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    /**
     * Returns the reporting application code.
     * 中文：返回上报应用编码。
     *
     * @return application code
     */
    public String getApplicationCode() {
        return applicationCode;
    }

    /**
     * Sets the reporting application code.
     * 中文：设置上报应用编码。
     *
     * @param applicationCode application code
     */
    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    /**
     * Returns the owning business code.
     * 中文：返回所属业务编码。
     *
     * @return business code
     */
    public String getBizCode() {
        return bizCode;
    }

    /**
     * Sets the owning business code.
     * 中文：设置所属业务编码。
     *
     * @param bizCode business code
     */
    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    /**
     * Returns the reporting application name.
     * 中文：返回上报应用名称。
     *
     * @return application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the reporting application name.
     * 中文：设置上报应用名称。
     *
     * @param applicationName application name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Returns the application environment.
     * 中文：返回应用运行环境。
     *
     * @return environment name
     */
    public String getEnv() {
        return env;
    }

    /**
     * Sets the application environment.
     * 中文：设置应用运行环境。
     *
     * @param env environment name
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * Returns the definition namespace.
     * 中文：返回接口定义命名空间。
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the definition namespace.
     * 中文：设置接口定义命名空间。
     *
     * @param namespace namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the reporting artifact version.
     * 中文：返回上报构件版本。
     *
     * @return artifact version
     */
    public String getArtifactVersion() {
        return artifactVersion;
    }

    /**
     * Sets the reporting artifact version.
     * 中文：设置上报构件版本。
     *
     * @param artifactVersion artifact version
     */
    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    /**
     * Returns the application build identifier.
     * 中文：返回应用构建标识。
     *
     * @return build identifier
     */
    public String getBuildId() {
        return buildId;
    }

    /**
     * Sets the application build identifier.
     * 中文：设置应用构建标识。
     *
     * @param buildId build identifier
     */
    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    /**
     * Returns an immutable snapshot of explicitly declared hosts.
     * 中文：返回显式声明主机列表的不可变快照。
     *
     * @return declared hosts
     */
    public List<String> getDeclaredHosts() {
        return List.copyOf(declaredHosts);
    }

    /**
     * Replaces the explicitly declared hosts with a defensive copy.
     * 中文：使用防御性副本替换显式声明的主机列表。
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
     * 中文：返回初次上报失败是否会阻止启动。
     *
     * @return {@code true} when fail-fast reporting is enabled
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether an initial reporting failure fails startup.
     * 中文：设置初次上报失败是否阻止启动。
     *
     * @param failFast whether fail-fast reporting is enabled
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Returns the reporting access key.
     * 中文：返回上报请求使用的访问密钥。
     *
     * @return access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Sets the reporting access key.
     * 中文：设置上报请求使用的访问密钥。
     *
     * @param accessKey access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Returns the reporting secret key.
     * 中文：返回上报请求使用的秘密密钥。
     *
     * @return secret key
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Sets the reporting secret key.
     * 中文：设置上报请求使用的秘密密钥。
     *
     * @param secretKey secret key
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Returns the Admin connection timeout.
     * 中文：返回 Admin 连接超时时间。
     *
     * @return connection timeout
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the Admin connection timeout.
     * 中文：设置 Admin 连接超时时间。
     *
     * @param connectTimeout connection timeout
     */
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Returns the Admin response read timeout.
     * 中文：返回 Admin 响应读取超时时间。
     *
     * @return read timeout
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the Admin response read timeout.
     * 中文：设置 Admin 响应读取超时时间。
     *
     * @param readTimeout read timeout
     */
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Returns the maximum consecutive short-backoff attempts.
     * 中文：返回连续短退避尝试的最大次数。
     *
     * @return maximum attempt count
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets the maximum consecutive short-backoff attempts.
     * 中文：设置连续短退避尝试的最大次数。
     *
     * @param maxAttempts maximum attempt count
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Returns the long reconciliation interval.
     * 中文：返回长期协调间隔。
     *
     * @return reconciliation interval
     */
    public Duration getReconcileInterval() {
        return reconcileInterval;
    }

    /**
     * Sets the long reconciliation interval.
     * 中文：设置长期协调间隔。
     *
     * @param reconcileInterval reconciliation interval
     */
    public void setReconcileInterval(Duration reconcileInterval) {
        this.reconcileInterval = reconcileInterval;
    }

    /**
     * Returns the local reporting state file path.
     * 中文：返回本地上报状态文件路径。
     *
     * @return state file path
     */
    public String getStateFile() {
        return stateFile;
    }

    /**
     * Sets the local reporting state file path.
     * 中文：设置本地上报状态文件路径。
     *
     * @param stateFile state file path
     */
    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }
}
