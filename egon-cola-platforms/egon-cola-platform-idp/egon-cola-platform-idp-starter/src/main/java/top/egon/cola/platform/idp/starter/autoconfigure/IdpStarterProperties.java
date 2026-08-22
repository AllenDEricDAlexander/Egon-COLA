package top.egon.cola.platform.idp.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
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

    /** Standard Spring OAuth2 Client machine-token settings. */
    private ServiceClient serviceClient = new ServiceClient();

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

    /** @return Spring OAuth2 Client service settings. */
    public ServiceClient getServiceClient() {
        return serviceClient;
    }

    /** @param serviceClient Spring OAuth2 Client service settings. */
    public void setServiceClient(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
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

    /** Standard Spring registration identity and cache renewal settings. */
    public static class ServiceClient {

        private String appId;
        private String registrationId;
        private Duration renewalSkew = Duration.ofSeconds(30);

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }

        public Duration getRenewalSkew() {
            return renewalSkew;
        }

        public void setRenewalSkew(Duration renewalSkew) {
            this.renewalSkew = renewalSkew;
        }

        /** Validates enabled service-client settings. */
        public void validate() {
            if (appId == null || appId.isBlank()
                    || registrationId == null || registrationId.isBlank()) {
                throw new IllegalStateException(
                        "egon.cola.platform.idp.service-client.app-id and registration-id are required"
                );
            }
            if (renewalSkew == null || renewalSkew.isNegative()) {
                throw new IllegalStateException(
                        "egon.cola.platform.idp.service-client.renewal-skew must be non-negative"
                );
            }
        }
    }
}
