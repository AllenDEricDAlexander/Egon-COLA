package top.egon.cola.platform.idp.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 描述普通 Servlet 资源服务器接入统一 IdP 时使用的配置。
 * 配置决定受信任的签发方、公钥来源、当前唯一 Resource，以及身份运行态的 Redis 键空间。
 *
 * <p>Describes the configuration used when a regular Servlet resource server integrates with
 * the unified IdP. The settings define the trusted issuer, public-key source, exact current
 * Resource, and Redis key spaces containing current identity runtime state.</p>
 */
@ConfigurationProperties("egon.cola.platform.idp")
public class IdpStarterProperties {

    /**
     * 是否启用 IdP Starter 自动装配。
     *
     * <p>Whether IdP Starter auto-configuration is enabled.</p>
     */
    private boolean enabled;

    /**
     * 是否把身份过滤器直接注册到 Servlet 容器。
     *
     * <p>Whether the identity filter is registered directly with the Servlet container.</p>
     */
    private boolean registerFilter = true;

    /**
     * 受信任 JWT 的签发方标识。
     *
     * <p>Issuer identifier required on trusted JWTs.</p>
     */
    private String issuer;

    /**
     * 获取 JWT 验签公钥的 JWK Set 地址。
     *
     * <p>JWK Set endpoint used to obtain JWT verification keys.</p>
     */
    private String jwkSetUri;

    /**
     * 当前应用唯一的 Resource Server 标识。
     *
     * <p>The sole Resource Server identifier of the current application.</p>
     */
    private String resourceServerId;

    /**
     * 当前应用唯一的 RFC 8707 Resource URI。
     *
     * <p>The sole RFC 8707 Resource URI of the current application.</p>
     */
    private URI resourceUri;

    /**
     * Fixed platform audience accepted by stateless USER access-token verification.
     */
    private String platformAudience = "platform";

    /**
     * Resource Server 运行态投影在 Redis 中使用的键前缀。
     *
     * <p>Redis key prefix used for Resource Server runtime projections.</p>
     */
    private String resourceStateKeyPrefix = "identity:resource-server:";

    /**
     * OAuth Client 运行态投影在 Redis 中使用的键前缀。
     *
     * <p>Redis key prefix used for OAuth Client runtime projections.</p>
     */
    private String oauthClientStateKeyPrefix = "identity:oauth-client:";

    /**
     * Resource Server 启动准入和机器身份配置。
     *
     * <p>Resource Server startup-admission and machine-identity settings.</p>
     */
    private Admission admission = new Admission();

    /**
     * 创建使用默认值初始化的 IdP Starter 配置。
     *
     * <p>Creates IdP Starter settings initialized with their defaults.</p>
     */
    public IdpStarterProperties() {
    }

    /**
     * 返回是否启用 Starter。
     *
     * <p>Returns whether the Starter is enabled.</p>
     *
     * @return {@code true} 表示启用；{@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 Starter。
     *
     * <p>Sets whether the Starter is enabled.</p>
     *
     * @param enabled 是否启用；whether to enable the Starter
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回是否由 Starter 注册 Servlet Filter。
     *
     * <p>Returns whether the Starter registers its Servlet filter.</p>
     *
     * @return {@code true} 表示自动注册；{@code true} when automatic registration is enabled
     */
    public boolean isRegisterFilter() {
        return registerFilter;
    }

    /**
     * 设置是否由 Starter 注册 Servlet Filter。
     *
     * <p>Sets whether the Starter registers its Servlet filter.</p>
     *
     * @param registerFilter 是否自动注册；whether automatic registration is enabled
     */
    public void setRegisterFilter(boolean registerFilter) {
        this.registerFilter = registerFilter;
    }

    /**
     * 返回受信任签发方。
     *
     * <p>Returns the trusted issuer.</p>
     *
     * @return 签发方标识；issuer identifier
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置受信任签发方。
     *
     * <p>Sets the trusted issuer.</p>
     *
     * @param issuer 签发方标识；issuer identifier
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 返回 JWK Set 地址。
     *
     * <p>Returns the JWK Set endpoint.</p>
     *
     * @return JWK Set 地址；JWK Set endpoint
     */
    public String getJwkSetUri() {
        return jwkSetUri;
    }

    /**
     * 设置 JWK Set 地址。
     *
     * <p>Sets the JWK Set endpoint.</p>
     *
     * @param jwkSetUri JWK Set 地址；JWK Set endpoint
     */
    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    /**
     * 返回当前应用的 Resource Server 标识。
     *
     * <p>Returns the Resource Server identifier of the current application.</p>
     *
     * @return Resource Server 标识；Resource Server identifier
     */
    public String getResourceServerId() {
        return resourceServerId;
    }

    /**
     * 设置当前应用的 Resource Server 标识。
     *
     * <p>Sets the Resource Server identifier of the current application.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     */
    public void setResourceServerId(String resourceServerId) {
        this.resourceServerId = resourceServerId;
    }

    /**
     * 返回当前应用唯一的 Resource URI。
     *
     * <p>Returns the sole Resource URI of the current application.</p>
     *
     * @return Resource URI；Resource URI
     */
    public URI getResourceUri() {
        return resourceUri;
    }

    /**
     * 设置当前应用唯一的 Resource URI。
     *
     * <p>Sets the sole Resource URI of the current application.</p>
     *
     * @param resourceUri Resource URI；Resource URI
     */
    public void setResourceUri(URI resourceUri) {
        this.resourceUri = resourceUri;
    }

    public String getPlatformAudience() {
        return platformAudience;
    }

    public void setPlatformAudience(String platformAudience) {
        this.platformAudience = platformAudience;
    }

    /**
     * 返回 Resource Server 投影的 Redis 键前缀。
     *
     * <p>Returns the Redis key prefix for Resource Server projections.</p>
     *
     * @return Redis 键前缀；Redis key prefix
     */
    public String getResourceStateKeyPrefix() {
        return resourceStateKeyPrefix;
    }

    /**
     * 设置 Resource Server 投影的 Redis 键前缀。
     *
     * <p>Sets the Redis key prefix for Resource Server projections.</p>
     *
     * @param resourceStateKeyPrefix Redis 键前缀；Redis key prefix
     */
    public void setResourceStateKeyPrefix(String resourceStateKeyPrefix) {
        this.resourceStateKeyPrefix = resourceStateKeyPrefix;
    }

    /**
     * 返回 OAuth Client 投影的 Redis 键前缀。
     *
     * <p>Returns the Redis key prefix for OAuth Client projections.</p>
     *
     * @return Redis 键前缀；Redis key prefix
     */
    public String getOauthClientStateKeyPrefix() {
        return oauthClientStateKeyPrefix;
    }

    /**
     * 设置 OAuth Client 投影的 Redis 键前缀。
     *
     * <p>Sets the Redis key prefix for OAuth Client projections.</p>
     *
     * @param oauthClientStateKeyPrefix Redis 键前缀；Redis key prefix
     */
    public void setOauthClientStateKeyPrefix(
            String oauthClientStateKeyPrefix
    ) {
        this.oauthClientStateKeyPrefix = oauthClientStateKeyPrefix;
    }

    /**
     * 返回 Resource Server 启动准入配置。
     *
     * <p>Returns the Resource Server startup-admission settings.</p>
     *
     * @return 准入配置；admission settings
     */
    public Admission getAdmission() {
        return admission;
    }

    /**
     * 设置 Resource Server 启动准入配置。
     *
     * <p>Sets the Resource Server startup-admission settings.</p>
     *
     * @param admission 准入配置；admission settings
     */
    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    /**
     * 校验启用身份验证所必需的全部配置。
     *
     * <p>Validates all settings required to enable identity verification.</p>
     *
     * @throws IllegalStateException 当必要配置缺失或 Resource URI 无效时；when a required
     *                               setting is missing or the Resource URI is invalid
     */
    public void validate() {
        required(issuer, "issuer");
        required(jwkSetUri, "jwkSetUri");
        required(resourceServerId, "resourceServerId");
        resource(resourceUri, "resourceUri");
        required(platformAudience, "platformAudience");
        required(resourceStateKeyPrefix, "resourceStateKeyPrefix");
        required(oauthClientStateKeyPrefix, "oauthClientStateKeyPrefix");
    }

    /**
     * 校验生产 Admission Ticket 供应器所需的全部机器身份配置。
     *
     * <p>Validates all machine-identity settings required by the production Admission Ticket
     * supplier.</p>
     *
     * @throws IllegalStateException 配置缺失、RPC 超时无效或私钥路径非绝对路径时抛出；when
     * settings are missing, the RPC timeout is invalid, or the private-key path is not absolute
     */
    public void validateAdmission() {
        if (admission == null) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp.admission is required"
            );
        }
        required(admission.bizCode, "admission.bizCode");
        required(admission.appCode, "admission.appCode");
        required(admission.environment, "admission.environment");
        required(admission.instanceId, "admission.instanceId");
        required(
                admission.managementClientId,
                "admission.managementClientId"
        );
        required(admission.kid, "admission.kid");
        if (admission.privateKeyPath == null
                || !admission.privateKeyPath.isAbsolute()) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp.admission.privateKeyPath must be absolute"
            );
        }
        required(admission.rpcTarget, "admission.rpcTarget");
        if (admission.rpcTimeout == null
                || admission.rpcTimeout.isZero()
                || admission.rpcTimeout.isNegative()) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp.admission.rpcTimeout must be positive"
            );
        }
        if (admission.renewalSkew == null
                || admission.renewalSkew.isZero()
                || admission.renewalSkew.isNegative()) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp.admission.renewalSkew must be positive"
            );
        }
    }

    /**
     * 校验单个文本配置不为空。
     *
     * <p>Validates that a scalar text setting is not blank.</p>
     *
     * @param value 配置值；setting value
     * @param field 配置字段名；setting field name
     * @throws IllegalStateException 当配置为空时；when the setting is blank
     */
    private void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp." + field + " is required");
        }
    }

    /**
     * 校验 Resource URI。
     *
     * <p>Validates a Resource URI.</p>
     *
     * @param value Resource URI；Resource URI
     * @param field 配置字段名；setting field name
     */
    private void resource(URI value, String field) {
        if (value == null
                || !value.isAbsolute()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp." + field
                            + " must be an absolute normalized URI without a fragment"
            );
        }
    }

    /**
     * Resource Server Admission Ticket 和 Management Client 配置。
     *
     * <p>Resource Server Admission Ticket and Management Client settings.</p>
     */
    public static class Admission {

        /** 业务域编码；business-domain code. */
        private String bizCode;

        /** 应用编码；application code. */
        private String appCode;

        /** 运行环境编码；runtime environment code. */
        private String environment;

        /** DDC 稳定实例标识；stable DDC instance identifier. */
        private String instanceId;

        /** Resource 绑定的 Confidential Management Client；Resource-bound confidential
         * Management Client. */
        private String managementClientId;

        /** Client JWK kid；Client JWK kid. */
        private String kid;

        /** owner-only PKCS#8 RSA 私钥绝对路径；absolute owner-only PKCS#8 RSA private-key path. */
        private Path privateKeyPath;

        /** IdP Admission Egon-RPC 静态目标；static IdP Admission Egon-RPC target. */
        private String rpcTarget;

        /** 单次 Admission RPC 超时；per-call Admission RPC timeout. */
        private Duration rpcTimeout = Duration.ofSeconds(3);

        /** 票据提前续签窗口；ticket renewal-ahead window. */
        private Duration renewalSkew = Duration.ofSeconds(30);

        /**
         * 创建空 Admission 配置供 Spring 绑定。
         *
         * <p>Creates empty Admission settings for Spring binding.</p>
         */
        public Admission() {
        }

        /** @return 业务域编码；business-domain code */
        public String getBizCode() {
            return bizCode;
        }

        /** @param bizCode 业务域编码；business-domain code */
        public void setBizCode(String bizCode) {
            this.bizCode = bizCode;
        }

        /** @return 应用编码；application code */
        public String getAppCode() {
            return appCode;
        }

        /** @param appCode 应用编码；application code */
        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }

        /** @return 环境编码；environment code */
        public String getEnvironment() {
            return environment;
        }

        /** @param environment 环境编码；environment code */
        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        /** @return DDC 实例标识；DDC instance identifier */
        public String getInstanceId() {
            return instanceId;
        }

        /** @param instanceId DDC 实例标识；DDC instance identifier */
        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        /** @return Management Client 标识；Management Client identifier */
        public String getManagementClientId() {
            return managementClientId;
        }

        /** @param managementClientId Management Client 标识；Management Client identifier */
        public void setManagementClientId(String managementClientId) {
            this.managementClientId = managementClientId;
        }

        /** @return Client JWK kid；Client JWK kid */
        public String getKid() {
            return kid;
        }

        /** @param kid Client JWK kid；Client JWK kid */
        public void setKid(String kid) {
            this.kid = kid;
        }

        /** @return 私钥绝对路径；absolute private-key path */
        public Path getPrivateKeyPath() {
            return privateKeyPath;
        }

        /** @param privateKeyPath 私钥绝对路径；absolute private-key path */
        public void setPrivateKeyPath(Path privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        /** @return IdP Admission RPC 目标；IdP Admission RPC target */
        public String getRpcTarget() {
            return rpcTarget;
        }

        /** @param rpcTarget IdP Admission RPC 目标；IdP Admission RPC target */
        public void setRpcTarget(String rpcTarget) {
            this.rpcTarget = rpcTarget;
        }

        /** @return 单次 RPC 超时；per-call RPC timeout */
        public Duration getRpcTimeout() {
            return rpcTimeout;
        }

        /** @param rpcTimeout 单次 RPC 超时；per-call RPC timeout */
        public void setRpcTimeout(Duration rpcTimeout) {
            this.rpcTimeout = rpcTimeout;
        }

        /** @return 提前续签窗口；renewal-ahead window */
        public Duration getRenewalSkew() {
            return renewalSkew;
        }

        /** @param renewalSkew 提前续签窗口；renewal-ahead window */
        public void setRenewalSkew(Duration renewalSkew) {
            this.renewalSkew = renewalSkew;
        }
    }
}
