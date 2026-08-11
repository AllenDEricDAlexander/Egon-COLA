package top.egon.cola.platform.idp.gateway.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 描述 Gateway 接入统一 IdP 时使用的验证与运行时配置。
 * 验证配置限定受信任的签发方、JWK 和 Resource 投影键空间；运行时配置用于建立独立的 Redis 状态连接。
 *
 * <p>Describes verification and runtime settings for integrating the Gateway with the unified IdP.
 * Verification settings constrain the trusted issuer, JWK source, and Resource projection key
 * space, while runtime settings establish the dedicated Redis state connection.</p>
 */
@ConfigurationProperties("egon.cola.platform.idp.gateway")
public class IdpGatewayAdapterProperties {

    /**
     * 是否启用 Gateway IdP 适配器。
     *
     * <p>Whether the Gateway IdP adapter is enabled.</p>
     */
    private boolean enabled;

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
     * IdP 用户实时状态在 Redis 中使用的键前缀。
     *
     * <p>Redis key prefix used for current IdP user-state projections.</p>
     */
    private String userStateKeyPrefix = "identity:v1:user:";

    /** Resource Server 状态键前缀；Resource Server state-key prefix. */
    private String resourceStateKeyPrefix = "identity:resource-server:";

    /** Resource 三元组索引键前缀；Resource triple-index key prefix. */
    private String resourceScopeKeyPrefix = "identity:resource-scope:";

    /** Resource URI 索引键前缀；Resource URI-index key prefix. */
    private String resourceUriKeyPrefix = "identity:resource-uri:";

    /** OAuth Client 状态键前缀；OAuth Client state-key prefix. */
    private String clientStateKeyPrefix = "identity:oauth-client:";

    /**
     * Gateway 专用 Redis 运行时连接配置。
     *
     * <p>Runtime connection settings for the Gateway-specific Redis client.</p>
     */
    private final Runtime runtime = new Runtime();

    /**
     * 创建使用默认值初始化的 Gateway IdP 适配器配置。
     *
     * <p>Creates Gateway IdP adapter settings initialized with their defaults.</p>
     */
    public IdpGatewayAdapterProperties() {
    }

    /**
     * 返回是否启用适配器。
     *
     * <p>Returns whether the adapter is enabled.</p>
     *
     * @return {@code true} 表示启用；{@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用适配器。
     *
     * <p>Sets whether the adapter is enabled.</p>
     *
     * @param enabled 是否启用；whether to enable the adapter
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
     * 返回用户实时状态 Redis 键前缀。
     *
     * <p>Returns the Redis key prefix for current user state.</p>
     *
     * @return Redis 键前缀；Redis key prefix
     */
    public String getUserStateKeyPrefix() {
        return userStateKeyPrefix;
    }

    /**
     * 设置用户实时状态 Redis 键前缀。
     *
     * <p>Sets the Redis key prefix for current user state.</p>
     *
     * @param userStateKeyPrefix Redis 键前缀；Redis key prefix
     */
    public void setUserStateKeyPrefix(String userStateKeyPrefix) {
        this.userStateKeyPrefix = userStateKeyPrefix;
    }

    /** 返回 Resource 状态键前缀；Returns the Resource state-key prefix. */
    public String getResourceStateKeyPrefix() {
        return resourceStateKeyPrefix;
    }

    /** 设置 Resource 状态键前缀；Sets the Resource state-key prefix. */
    public void setResourceStateKeyPrefix(String resourceStateKeyPrefix) {
        this.resourceStateKeyPrefix = resourceStateKeyPrefix;
    }

    /** 返回 Resource 三元组索引前缀；Returns the Resource triple-index prefix. */
    public String getResourceScopeKeyPrefix() {
        return resourceScopeKeyPrefix;
    }

    /** 设置 Resource 三元组索引前缀；Sets the Resource triple-index prefix. */
    public void setResourceScopeKeyPrefix(String resourceScopeKeyPrefix) {
        this.resourceScopeKeyPrefix = resourceScopeKeyPrefix;
    }

    /** 返回 Resource URI 索引前缀；Returns the Resource URI-index prefix. */
    public String getResourceUriKeyPrefix() {
        return resourceUriKeyPrefix;
    }

    /** 设置 Resource URI 索引前缀；Sets the Resource URI-index prefix. */
    public void setResourceUriKeyPrefix(String resourceUriKeyPrefix) {
        this.resourceUriKeyPrefix = resourceUriKeyPrefix;
    }

    /** 返回 OAuth Client 状态键前缀；Returns the OAuth Client state-key prefix. */
    public String getClientStateKeyPrefix() {
        return clientStateKeyPrefix;
    }

    /** 设置 OAuth Client 状态键前缀；Sets the OAuth Client state-key prefix. */
    public void setClientStateKeyPrefix(String clientStateKeyPrefix) {
        this.clientStateKeyPrefix = clientStateKeyPrefix;
    }

    /**
     * 返回 Gateway 专用 Redis 运行时配置。
     *
     * <p>Returns the Gateway-specific Redis runtime settings.</p>
     *
     * @return 运行时配置；runtime settings
     */
    public Runtime getRuntime() {
        return runtime;
    }

    /**
     * 校验启用 Gateway IdP 验证所必需的配置。
     *
     * <p>Validates settings required to enable Gateway IdP verification.</p>
     *
     * @throws IllegalStateException 当必要配置缺失或集合包含空值时；when a required setting is
     *                               missing or a configured set contains a blank value
     */
    public void validate() {
        required(issuer, "issuer");
        required(jwkSetUri, "jwkSetUri");
        required(userStateKeyPrefix, "userStateKeyPrefix");
        required(resourceStateKeyPrefix, "resourceStateKeyPrefix");
        required(resourceScopeKeyPrefix, "resourceScopeKeyPrefix");
        required(resourceUriKeyPrefix, "resourceUriKeyPrefix");
        required(clientStateKeyPrefix, "clientStateKeyPrefix");
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
                    "egon.cola.platform.idp.gateway." + field + " is required");
        }
    }

    /**
     * 描述 Gateway 读取 IdP 用户实时状态所需的 Redis 连接参数。
     * 密码通过文件路径注入，避免把明文凭据直接放入常规配置值。
     *
     * <p>Describes Redis connection settings used by the Gateway to read current IdP user state.
     * The password is supplied through a file path so plaintext credentials need not be stored in
     * ordinary configuration values.</p>
     */
    public static class Runtime {

        /**
         * 是否创建 Gateway 专用 Redis 客户端。
         *
         * <p>Whether the Gateway-specific Redis client is created.</p>
         */
        private boolean redisEnabled = true;

        /**
         * Redis 单节点连接地址。
         *
         * <p>Redis single-server connection address.</p>
         */
        private String redisAddress = "redis://127.0.0.1:6379";

        /**
         * Redis 逻辑数据库编号。
         *
         * <p>Redis logical database index.</p>
         */
        private int database;

        /**
         * 包含 Redis 密码的本地文件路径。
         *
         * <p>Local file path containing the Redis password.</p>
         */
        private String passwordFile;

        /**
         * Redis 命令超时时间。
         *
         * <p>Redis command timeout.</p>
         */
        private Duration timeout = Duration.ofSeconds(2);

        /**
         * 创建使用默认值初始化的 Redis 运行时配置。
         *
         * <p>Creates Redis runtime settings initialized with their defaults.</p>
         */
        public Runtime() {
        }

        /**
         * 返回是否启用 Redis 运行时连接。
         *
         * <p>Returns whether the Redis runtime connection is enabled.</p>
         *
         * @return {@code true} 表示启用；{@code true} when enabled
         */
        public boolean isRedisEnabled() {
            return redisEnabled;
        }

        /**
         * 设置是否启用 Redis 运行时连接。
         *
         * <p>Sets whether the Redis runtime connection is enabled.</p>
         *
         * @param redisEnabled 是否启用；whether to enable the Redis connection
         */
        public void setRedisEnabled(boolean redisEnabled) {
            this.redisEnabled = redisEnabled;
        }

        /**
         * 返回 Redis 地址。
         *
         * <p>Returns the Redis address.</p>
         *
         * @return Redis 地址；Redis address
         */
        public String getRedisAddress() {
            return redisAddress;
        }

        /**
         * 设置 Redis 地址。
         *
         * <p>Sets the Redis address.</p>
         *
         * @param redisAddress Redis 地址；Redis address
         */
        public void setRedisAddress(String redisAddress) {
            this.redisAddress = redisAddress;
        }

        /**
         * 返回 Redis 逻辑数据库编号。
         *
         * <p>Returns the Redis logical database index.</p>
         *
         * @return 数据库编号；database index
         */
        public int getDatabase() {
            return database;
        }

        /**
         * 设置 Redis 逻辑数据库编号。
         *
         * <p>Sets the Redis logical database index.</p>
         *
         * @param database 数据库编号；database index
         */
        public void setDatabase(int database) {
            this.database = database;
        }

        /**
         * 返回 Redis 密码文件路径。
         *
         * <p>Returns the Redis password-file path.</p>
         *
         * @return 密码文件路径；password-file path
         */
        public String getPasswordFile() {
            return passwordFile;
        }

        /**
         * 设置 Redis 密码文件路径。
         *
         * <p>Sets the Redis password-file path.</p>
         *
         * @param passwordFile 密码文件路径；password-file path
         */
        public void setPasswordFile(String passwordFile) {
            this.passwordFile = passwordFile;
        }

        /**
         * 返回 Redis 命令超时时间。
         *
         * <p>Returns the Redis command timeout.</p>
         *
         * @return 超时时间；timeout duration
         */
        public Duration getTimeout() {
            return timeout;
        }

        /**
         * 设置 Redis 命令超时时间。
         *
         * <p>Sets the Redis command timeout.</p>
         *
         * @param timeout 超时时间；timeout duration
         */
        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
