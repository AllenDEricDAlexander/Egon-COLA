package top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;
import org.springframework.validation.annotation.Validated;
import java.util.Set;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 描述普通 Servlet 资源服务器接入统一 Tianquan-Shoubing 时使用的配置。
 * 配置决定受信任的签发方、公钥来源、当前唯一 Resource，以及身份运行态的 Redis 键空间。
 *
 * <p>Describes the configuration used when a regular Servlet resource server integrates with
 * the unified Tianquan-Shoubing. The settings define the trusted issuer, public-key source, exact current
 * Resource, and Redis key spaces containing current identity runtime state.</p>
 */
@Validated
@ConfigurationProperties("egon.cola.platform.tianquan.shoubing")
public class IdpStarterProperties {

    /**
     * 是否启用 Tianquan-Shoubing Starter 自动装配。
     *
     * <p>Whether Tianquan-Shoubing Starter auto-configuration is enabled.</p>
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

    /** Exact issuer settings trusted by multi-issuer Resource Server verification. */
    private Map<String, TrustedIssuerSettings> trustedIssuers = new LinkedHashMap<>();

    /** Clock skew applied to every locally verified issuer. */
    private Duration clockSkew = Duration.ofSeconds(30);

    /** Strict shared revocation state configuration. */
    private Revocation revocation = new Revocation();

    /** Bounds the number of configured issuer decoders and their JWK cache lifetime. */
    private JwksCache jwksCache = new JwksCache();

    /** Optional, explicitly bounded legacy-token cutover configuration. */
    private Migration migration = new Migration();

    /** Standard Spring OAuth2 Client machine-token settings. */
    private ServiceClient serviceClient = new ServiceClient();

    /** Local RPC selectors and provider policies; credentials never come from request metadata. */
    @Valid
    @NotNull
    private RpcProperties rpc = new RpcProperties();

    public RpcProperties getRpc() {
        return rpc;
    }

    public void setRpc(RpcProperties rpc) {
        this.rpc = Objects.requireNonNull(rpc, "rpc");
    }

    /**
     * 创建使用默认值初始化的 Tianquan-Shoubing Starter 配置。
     *
     * <p>Creates Tianquan-Shoubing Starter settings initialized with their defaults.</p>
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

    public Map<String, TrustedIssuerSettings> getTrustedIssuers() {
        return trustedIssuers;
    }

    public void setTrustedIssuers(Map<String, TrustedIssuerSettings> trustedIssuers) {
        this.trustedIssuers = trustedIssuers == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(trustedIssuers);
    }

    public Revocation getRevocation() {
        return revocation;
    }

    public void setRevocation(Revocation revocation) {
        this.revocation = Objects.requireNonNull(revocation, "revocation");
    }

    public JwksCache getJwksCache() {
        return jwksCache;
    }

    public void setJwksCache(JwksCache jwksCache) {
        this.jwksCache = Objects.requireNonNull(jwksCache, "jwksCache");
    }

    public Migration getMigration() {
        return migration;
    }

    public void setMigration(Migration migration) {
        this.migration = Objects.requireNonNull(migration, "migration");
    }

    public boolean isMultiIssuerConfigured() {
        return !trustedIssuers.isEmpty();
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
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
        if (clockSkew == null || clockSkew.isNegative()
                || clockSkew.compareTo(Duration.ofSeconds(60)) > 0) {
            throw invalid("clock-skew must be between 0 and 60 seconds");
        }
        migration.validate();
        if (trustedIssuers.isEmpty()) {
            required(issuer, "issuer");
            required(jwkSetUri, "jwkSetUri");
        } else {
            if (trustedIssuers.size() > jwksCache.maxIssuers) {
                throw invalid("trusted-issuers exceeds jwks-cache.max-issuers");
            }
            trustedIssuers.forEach((key, value) -> {
                required(key, "trusted-issuers map key");
                Objects.requireNonNull(value, "trusted issuer entry").validate(key);
            });
            revocation.validate();
            jwksCache.validate();
        }
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
    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "egon.cola.platform.tianquan.shoubing." + field + " is required");
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
                    "egon.cola.platform.tianquan.shoubing." + field
                            + " must be an absolute normalized URI without a fragment"
            );
        }
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException(
                "egon.cola.platform.tianquan.shoubing." + message);
    }

    /** Mutable binding value validated before it enters the trusted issuer registry. */
    public static class TrustedIssuerSettings {

        private String issuer;
        private String jwksUri;
        private String realmKey;
        private String realmType;
        private String tenantId;
        private String status = "ACTIVE";
        private long trustVersion;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(String jwksUri) {
            this.jwksUri = jwksUri;
        }

        public String getRealmKey() {
            return realmKey;
        }

        public void setRealmKey(String realmKey) {
            this.realmKey = realmKey;
        }

        public String getRealmType() {
            return realmType;
        }

        public void setRealmType(String realmType) {
            this.realmType = realmType;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getTrustVersion() {
            return trustVersion;
        }

        public void setTrustVersion(long trustVersion) {
            this.trustVersion = trustVersion;
        }

        public void validate(String mapKey) {
            if (mapKey == null || !mapKey.matches("[A-Za-z0-9._-]{1,128}")) {
                throw invalid("trusted-issuers map key must be a stable ASCII identifier");
            }
            URI issuerUri = secureUri(issuer, "issuer");
            URI jwks = secureUri(jwksUri, "jwks-uri");
            if (!issuerUri.toASCIIString().equals(issuer)) {
                throw invalid("trusted issuer value must be an exact canonical URI");
            }
            if (!sameOrigin(issuerUri, jwks)) {
                throw invalid("trusted issuer JWKS URI must use the issuer origin");
            }
            if (realmKey == null || !realmKey.matches("[A-Za-z0-9:_-]{1,128}")) {
                throw invalid("trusted issuer realm-key is invalid");
            }
            if (!"TENANT".equals(realmType) && !"PLATFORM".equals(realmType)) {
                throw invalid("trusted issuer realm-type must be TENANT or PLATFORM");
            }
            if ("TENANT".equals(realmType)) {
                if (tenantId == null || !tenantId.matches("[1-9][0-9]{0,18}")) {
                    throw invalid("TENANT issuer requires a positive decimal tenant-id");
                }
                try {
                    if (Long.parseLong(tenantId) <= 0L) {
                        throw invalid("TENANT issuer requires a positive tenant-id");
                    }
                } catch (NumberFormatException failure) {
                    throw invalid("TENANT issuer tenant-id is outside Long range");
                }
            } else if (tenantId != null) {
                throw invalid("PLATFORM issuer must not carry tenant-id");
            }
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                throw invalid("trusted issuer status must be ACTIVE or INACTIVE");
            }
            if (trustVersion < 0L) {
                throw invalid("trusted issuer trust-version must be non-negative");
            }
        }
    }

    public static class Revocation {

        private String primaryEndpoint;
        private String mode = "STRICT_SHARED";
        private Duration timeout = Duration.ofMillis(500);
        private String epoch;

        public String getPrimaryEndpoint() {
            return primaryEndpoint;
        }

        public void setPrimaryEndpoint(String primaryEndpoint) {
            this.primaryEndpoint = primaryEndpoint;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getEpoch() {
            return epoch;
        }

        public void setEpoch(String epoch) {
            this.epoch = epoch;
        }

        private void validate() {
            URI endpoint = secureRedisUri(primaryEndpoint, "revocation.primary-endpoint");
            if (!"STRICT_SHARED".equals(mode)) {
                throw invalid("revocation.mode must be STRICT_SHARED");
            }
            if (timeout == null || timeout.isZero() || timeout.isNegative()
                    || timeout.compareTo(Duration.ofSeconds(5)) > 0) {
                throw invalid("revocation.timeout must be between 1ms and 5s");
            }
            if (epoch == null || !epoch.matches("[A-Za-z0-9._-]{1,128}")) {
                throw invalid("revocation.epoch is required");
            }
            if (endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
                throw invalid("revocation.primary-endpoint must not contain credentials or fragment");
            }
        }
    }

    public static class JwksCache {

        private Duration ttl = Duration.ofSeconds(300);
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);
        private int maxIssuers = 1000;

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
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

        public int getMaxIssuers() {
            return maxIssuers;
        }

        public void setMaxIssuers(int maxIssuers) {
            this.maxIssuers = maxIssuers;
        }

        private void validate() {
            if (ttl == null || ttl.isNegative() || ttl.isZero()
                    || ttl.compareTo(Duration.ofHours(1)) > 0) {
                throw invalid("jwks-cache.ttl must be between 1ms and 1h");
            }
            if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                    || connectTimeout.compareTo(Duration.ofSeconds(10)) > 0
                    || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()
                    || readTimeout.compareTo(Duration.ofSeconds(10)) > 0) {
                throw invalid("jwks-cache connect/read timeouts must be between 1ms and 10s");
            }
            if (maxIssuers < 1 || maxIssuers > 1000) {
                throw invalid("jwks-cache.max-issuers must be between 1 and 1000");
            }
        }
    }

    public static class Migration {

        private String legacyIssuer;
        private String legacyTenantAllowlistKey;

        public String getLegacyIssuer() {
            return legacyIssuer;
        }

        public void setLegacyIssuer(String legacyIssuer) {
            this.legacyIssuer = legacyIssuer;
        }

        public String getLegacyTenantAllowlistKey() {
            return legacyTenantAllowlistKey;
        }

        public void setLegacyTenantAllowlistKey(String legacyTenantAllowlistKey) {
            this.legacyTenantAllowlistKey = legacyTenantAllowlistKey;
        }

        private void validate() {
            boolean issuerConfigured = legacyIssuer != null && !legacyIssuer.isBlank();
            boolean keyConfigured = legacyTenantAllowlistKey != null
                    && !legacyTenantAllowlistKey.isBlank();
            if (issuerConfigured != keyConfigured) {
                throw invalid("migration.legacy-issuer and legacy-tenant-allowlist-key must be configured together");
            }
            if (issuerConfigured) {
                secureUri(legacyIssuer, "migration.legacy-issuer");
                if (!legacyTenantAllowlistKey.matches("[A-Za-z0-9:_.-]{1,256}")) {
                    throw invalid("migration.legacy-tenant-allowlist-key is invalid");
                }
            }
        }
    }

    private static URI secureUri(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " is required");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException malformed) {
            throw invalid(field + " is invalid");
        }
        if (!uri.isAbsolute() || uri.isOpaque() || uri.getHost() == null
                || !("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme()) && isLoopbackHost(uri.getHost()))
                || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !uri.normalize().equals(uri)
                || !uri.toASCIIString().equals(value)) {
            throw invalid(field + " must be HTTPS or exact loopback HTTP and canonical");
        }
        return uri;
    }

    private static URI secureRedisUri(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " is required");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException malformed) {
            throw invalid(field + " is invalid");
        }
        if (!uri.isAbsolute() || uri.isOpaque() || uri.getHost() == null
                || !("rediss".equalsIgnoreCase(uri.getScheme())
                || "redis".equalsIgnoreCase(uri.getScheme()) && isLoopbackHost(uri.getHost()))
                || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !uri.normalize().equals(uri)
                || !uri.toASCIIString().equals(value)) {
            throw invalid(field + " must be rediss or exact loopback redis and canonical");
        }
        return uri;
    }

    private static boolean isLoopbackHost(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host) || "[::1]".equals(host);
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return 6379;
    }

    /** Standard Spring registration identity and cache renewal settings. */
    public static class ServiceClient {

        private String appId;
        private String registrationId;
        private Map<String, Long> credentialVersions = new LinkedHashMap<>();
        private Map<String, String> tenantRegistrationIds = new LinkedHashMap<>();
        private Duration renewalSkew = Duration.ofSeconds(60);

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

        public Map<String, Long> getCredentialVersions() {
            return credentialVersions;
        }

        public void setCredentialVersions(Map<String, Long> credentialVersions) {
            this.credentialVersions = credentialVersions == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(credentialVersions);
        }

        /** Resolves the explicitly configured credential generation for one registration. */
        public long credentialVersion(String registrationId) {
            if (registrationId == null || registrationId.isBlank()) {
                throw new IllegalStateException("service-client registrationId is required");
            }
            Long version = credentialVersions.get(registrationId);
            if (version == null) {
                throw new IllegalStateException(
                        "egon.cola.platform.tianquan.shoubing.service-client.credential-versions."
                                + registrationId + " is required");
            }
            if (version < 0L) {
                throw invalid("service-client credentialVersion must be non-negative");
            }
            return version;
        }

        public Map<String, String> getTenantRegistrationIds() {
            return tenantRegistrationIds;
        }

        public void setTenantRegistrationIds(Map<String, String> tenantRegistrationIds) {
            this.tenantRegistrationIds = tenantRegistrationIds == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(tenantRegistrationIds);
        }

        /** Selects only an explicitly configured tenant registration; an empty map keeps single-registration mode. */
        public String registrationIdForTenant(String tenantId) {
            if (tenantRegistrationIds.isEmpty()) {
                return registrationId;
            }
            String selected = tenantRegistrationIds.get(tenantId);
            if (selected == null) {
                throw invalid("service-client tenant registration is not configured");
            }
            return selected;
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
                        "egon.cola.platform.tianquan.shoubing.service-client.app-id and registration-id are required"
                );
            }
            if (renewalSkew == null || renewalSkew.isNegative()) {
                throw new IllegalStateException(
                        "egon.cola.platform.tianquan.shoubing.service-client.renewal-skew must be non-negative"
                );
            }
            if (tenantRegistrationIds.size() > 1000) {
                throw invalid("service-client tenant-registration-ids exceeds the issuer bound");
            }
            java.util.Set<String> registrations = new java.util.HashSet<>();
            tenantRegistrationIds.forEach((tenantId, registration) -> {
                if (tenantId == null || !tenantId.matches("[1-9][0-9]{0,18}")
                        || registration == null || !registration.matches("[A-Za-z0-9._-]{1,128}")) {
                    throw invalid("service-client tenant-registration-ids entry is invalid");
                }
                try {
                    Long.parseLong(tenantId);
                } catch (NumberFormatException overflow) {
                    throw invalid("service-client tenant-registration-ids tenant is outside Long range");
                }
                if (!registrations.add(registration)) {
                    throw invalid("service-client tenant registrations must be distinct");
                }
                credentialVersion(registration);
            });
            credentialVersions.forEach((key, version) -> {
                if (key == null || !key.matches("[A-Za-z0-9._-]{1,128}")
                        || version == null || version < 0L) {
                    throw invalid("service-client credential-versions entry is invalid");
                }
            });
        }
    }

    public enum RpcConsumerModeEnum {
        USER_RELAY("USER_RELAY"), SERVICE("SERVICE");
        private final String code;
        RpcConsumerModeEnum(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    public enum RpcPrincipalKindEnum {
        USER("USER"), SERVICE("SERVICE"), USER_OR_SERVICE("USER_OR_SERVICE");
        private final String code;
        RpcPrincipalKindEnum(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class RpcProperties {
        private boolean enabled;
        @Valid @NotNull @Builder.Default
        private RpcConsumerProperties consumer = new RpcConsumerProperties();
        @Valid @NotNull @Builder.Default
        private RpcProviderProperties provider = new RpcProviderProperties();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class RpcConsumerProperties {
        private boolean enabled;
        @Valid @NotNull @Size(max = 64) @Builder.Default
        private Map<@NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String, @NotNull @Valid RpcConsumerBindingProperties> bindings = new LinkedHashMap<>();

        @AssertTrue
        public boolean isConfigurationValid() {
            if (!enabled) { return true; }
            if (bindings == null || bindings.isEmpty()) { return false; }
            var selectors = new java.util.HashSet<String>();
            for (var binding : bindings.values()) {
                if (binding == null || !selectors.add(binding.getServiceName() + "|" + binding.getGroup() + "|" + binding.getVersion()
                        + "|" + binding.getFullMethodName() + "|" + (binding.getProcessIdentity() == null ? null
                        : binding.getProcessIdentity().get("applicationName") + "|" + binding.getProcessIdentity().get("env")))) { return false; }
            }
            return true;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class RpcConsumerBindingProperties {
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
        private String serviceName;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
        private String group;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
        private String version;
        @NotBlank @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_.]{0,127}/[A-Za-z_][A-Za-z0-9_]{0,127}")
        private String fullMethodName;
        @NotNull @Size(min = 2, max = 2) @Builder.Default
        private Map<String, String> processIdentity = new LinkedHashMap<>();
        @NotNull @Builder.Default
        private RpcConsumerModeEnum mode = RpcConsumerModeEnum.USER_RELAY;
        @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
        private String registrationId;
        @NotNull @Size(max = 64) @Builder.Default
        private Map<@NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String, @NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String> tenantRegistrationIds = new LinkedHashMap<>();
        @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
        private String appId;
        private URI resourceUri;
        private ServiceTokenContext scopeContext;
        @NotNull @Size(max = 64) @Builder.Default
        private Set<@NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String> scopes = Set.of();

        @AssertTrue
        public boolean isSelectorValid() {
            return processIdentity != null && processIdentity.keySet().equals(Set.of("applicationName", "env"))
                    && processIdentity.values().stream().allMatch(value -> value != null && value.matches("[A-Za-z0-9._:-]{1,128}"));
        }

        @AssertTrue
        public boolean isServiceRequestValid() {
            if (mode == RpcConsumerModeEnum.USER_RELAY) { return true; }
            if (mode != RpcConsumerModeEnum.SERVICE || appId == null || scopes == null || scopes.isEmpty() || !rpcHttps(resourceUri)) { return false; }
            if (scopeContext == ServiceTokenContext.PLATFORM) { return registrationId != null && !registrationId.isBlank()
                    && tenantRegistrationIds != null && tenantRegistrationIds.isEmpty(); }
            return scopeContext == ServiceTokenContext.TENANT && registrationId == null && tenantRegistrationIds != null && !tenantRegistrationIds.isEmpty();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class RpcProviderProperties {
        private boolean enabled;
        @Valid @NotNull @Size(max = 256) @Builder.Default
        private Map<@NotNull @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_.]{0,127}/[A-Za-z_][A-Za-z0-9_]{0,127}") String, @NotNull @Valid RpcProviderMethodProperties> methods = new LinkedHashMap<>();
        @AssertTrue
        public boolean isConfigurationValid() { return !enabled || methods != null && !methods.isEmpty(); }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class RpcProviderMethodProperties {
        @Builder.Default
        private boolean required = true;
        @NotNull @Builder.Default
        private RpcPrincipalKindEnum principalKind = RpcPrincipalKindEnum.USER_OR_SERVICE;
        private ServiceTokenContext scopeContext;
        @NotNull @Size(max = 64) @Builder.Default
        private Set<@NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String> requiredScopes = Set.of();
        private URI expectedResourceUri;
        @AssertTrue
        public boolean isRequestValid() {
            return rpcHttps(expectedResourceUri) && scopeContext != null && requiredScopes != null && !requiredScopes.isEmpty()
                    && !(principalKind == RpcPrincipalKindEnum.USER && scopeContext == ServiceTokenContext.PLATFORM);
        }
    }


    private static boolean rpcHttps(URI value) {
        return value != null && value.isAbsolute() && "https".equals(value.getScheme()) && value.getHost() != null
                && value.getUserInfo() == null && value.getQuery() == null && value.getFragment() == null
                && value.normalize().equals(value) && value.toASCIIString().length() <= 512;
    }
}
