package top.egon.cola.component.ddc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 绑定 DDC Starter 的作用域、管理端、Redis、实例和一致性配置。 Binds scope, management, Redis, instance, and consistency settings for the DDC starter.
 */
@ConfigurationProperties(prefix = "egon.cola.component.ddc", ignoreInvalidFields = true)
public class DdcProperties {

    /**
     * 是否启用 DDC Starter。 Whether the DDC starter is enabled.
     */
    private boolean enabled;

    /**
     * 当前应用编码。 Current application code.
     */
    private String appCode = "default-app";

    /**
     * 当前业务编码。 Current business code.
     */
    private String bizCode;

    /**
     * 当前部署环境编码。 Current deployment-environment code.
     */
    private String env = "dev";

    /**
     * 兼容旧作用域模型的命名空间编码。 Namespace code retained for the legacy scope model.
     */
    private String namespace = "default";

    /**
     * 引导阶段允许加载的远程 YAML 最大字节数。 Maximum remote YAML bytes allowed during bootstrap.
     */
    private long maxConfigBytes = 1024L * 1024L;

    /**
     * 管理端连接与签名配置。 Management connection and signing settings.
     */
    private Admin admin = new Admin();

    /**
     * Redis 拓扑与认证配置。 Redis topology and authentication settings.
     */
    private Redis redis = new Redis();

    /**
     * 配置客户端实例租约设置。 Configuration-client instance lease settings.
     */
    private Instance instance = new Instance();

    /**
     * 服务注册客户端设置。 Service-registry client settings.
     */
    private Registry registry = new Registry();

    /**
     * 刷新一致性与对账设置。 Refresh consistency and reconciliation settings.
     */
    private Consistency consistency = new Consistency();

    /**
     * 返回是否启用 DDC。 Returns whether DDC is enabled.
     *
     * @return DDC 启用标志。 DDC enabled flag
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 DDC。 Sets whether DDC is enabled.
     *
     * @param enabled 启用标志。 enabled flag
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回当前应用编码。 Returns the current application code.
     *
     * @return 应用编码。 application code
     */
    public String getAppCode() {
        return appCode;
    }

    /**
     * 设置当前应用编码。 Sets the current application code.
     *
     * @param appCode 应用编码。 application code
     */
    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    /**
     * 返回当前业务编码。 Returns the current business code.
     *
     * @return 业务编码。 business code
     */
    public String getBizCode() {
        return bizCode;
    }

    /**
     * 设置当前业务编码。 Sets the current business code.
     *
     * @param bizCode 业务编码。 business code
     */
    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    /**
     * 返回当前环境编码。 Returns the current environment code.
     *
     * @return 环境编码。 environment code
     */
    public String getEnv() {
        return env;
    }

    /**
     * 设置当前环境编码。 Sets the current environment code.
     *
     * @param env 环境编码。 environment code
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 返回兼容旧模型的命名空间编码。 Returns the namespace code retained for the legacy model.
     *
     * @return 命名空间编码。 namespace code
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置兼容旧模型的命名空间编码。 Sets the namespace code for the legacy model.
     *
     * @param namespace 命名空间编码。 namespace code
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 返回远程 YAML 最大字节数。 Returns the maximum remote YAML size in bytes.
     *
     * @return 最大字节数。 maximum byte count
     */
    public long getMaxConfigBytes() {
        return maxConfigBytes;
    }

    /**
     * 设置远程 YAML 最大字节数。 Sets the maximum remote YAML size in bytes.
     *
     * @param maxConfigBytes 最大字节数。 maximum byte count
     */
    public void setMaxConfigBytes(long maxConfigBytes) {
        this.maxConfigBytes = maxConfigBytes;
    }

    /**
     * 返回管理端配置。 Returns the management settings.
     *
     * @return 管理端配置。 management settings
     */
    public Admin getAdmin() {
        return admin;
    }

    /**
     * 替换管理端配置组。 Replaces the management settings group.
     *
     * @param admin 管理端配置。 management settings
     */
    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    /**
     * 返回 Redis 配置。 Returns the Redis settings.
     *
     * @return Redis 配置。 Redis settings
     */
    public Redis getRedis() {
        return redis;
    }

    /**
     * 替换 Redis 配置组。 Replaces the Redis settings group.
     *
     * @param redis Redis 配置。 Redis settings
     */
    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    /**
     * 返回客户端实例配置。 Returns the client instance settings.
     *
     * @return 实例配置。 instance settings
     */
    public Instance getInstance() {
        return instance;
    }

    /**
     * 替换客户端实例配置组。 Replaces the client instance settings group.
     *
     * @param instance 实例配置。 instance settings
     */
    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    /**
     * 返回服务注册配置。 Returns the service-registry settings.
     *
     * @return 服务注册配置。 service-registry settings
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * 替换服务注册配置组。 Replaces the service-registry settings group.
     *
     * @param registry 服务注册配置。 service-registry settings
     */
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    /**
     * 返回一致性配置。 Returns the consistency settings.
     *
     * @return 一致性配置。 consistency settings
     */
    public Consistency getConsistency() {
        return consistency;
    }

    /**
     * 替换一致性配置组。 Replaces the consistency settings group.
     *
     * @param consistency 一致性配置。 consistency settings
     */
    public void setConsistency(Consistency consistency) {
        this.consistency = consistency;
    }

    /**
     * 定义管理端地址、签名凭据、超时和 TLS 设置。 Defines management endpoint, signing credentials, timeouts, and TLS settings.
     */
    public static class Admin {

        /**
         * DDC 管理端根 URI。 Root URI of the DDC management service.
         */
        private String endpoint;

        /**
         * HMAC 签名访问密钥。 Access key used for HMAC signing.
         */
        private String accessKey;

        /**
         * HMAC 签名密钥。 Secret key used for HMAC signing.
         */
        private String secretKey;

        /**
         * 是否为 OpenAPI 请求生成签名。 Whether OpenAPI requests are signed.
         */
        private boolean signatureEnabled;

        /**
         * 建立管理端连接的超时时间。 Timeout for establishing management connections.
         */
        private Duration connectTimeout = Duration.ofSeconds(3);

        /**
         * 等待管理端响应的读取超时时间。 Timeout for reading management responses.
         */
        private Duration readTimeout = Duration.ofSeconds(10);

        /**
         * 管理端传输层安全设置。 Management transport-security settings.
         */
        private Tls tls = new Tls();

        /**
         * 返回原始管理端地址。 Returns the raw management endpoint.
         *
         * @return 原始管理端地址。 raw management endpoint
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * 设置管理端地址。 Sets the management endpoint.
         *
         * @param endpoint HTTP 或 HTTPS 根 URI。 HTTP or HTTPS root URI
         */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * 校验管理端地址并去除末尾斜杠。 Validates the management endpoint and removes its trailing slash.
         *
         * @return 规范化管理端根 URI。 normalized management root URI
         * @throws IllegalArgumentException 地址缺失、格式无效或包含路径、查询、片段、用户信息时抛出。 thrown when absent, malformed, or containing a path, query, fragment, or user info
         */
        public String requireEndpoint() {
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalArgumentException(
                        "egon.cola.component.ddc.admin.endpoint is required"
                );
            }
            URI uri;
            try {
                uri = URI.create(endpoint.trim());
            } catch (IllegalArgumentException exception) {
                throw invalidEndpoint(exception);
            }
            boolean rootPath = uri.getPath() == null
                    || uri.getPath().isBlank()
                    || "/".equals(uri.getPath());
            if (!(("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && rootPath
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && uri.getUserInfo() == null)) {
                throw invalidEndpoint(null);
            }
            String normalized = endpoint.trim();
            return normalized.endsWith("/")
                    ? normalized.substring(0, normalized.length() - 1)
                    : normalized;
        }

        /**
         * 创建统一的管理端地址校验异常。 Creates the common management-endpoint validation exception.
         *
         * @param cause 可选解析根因。 optional parsing cause
         * @return 地址校验异常。 endpoint validation exception
         */
        private IllegalArgumentException invalidEndpoint(Throwable cause) {
            String message =
                    "egon.cola.component.ddc.admin.endpoint "
                            + "must be an HTTP or HTTPS root URI";
            return cause == null
                    ? new IllegalArgumentException(message)
                    : new IllegalArgumentException(message, cause);
        }

        /**
         * 返回 HMAC 访问密钥。 Returns the HMAC access key.
         *
         * @return 访问密钥。 access key
         */
        public String getAccessKey() {
            return accessKey;
        }

        /**
         * 设置 HMAC 访问密钥。 Sets the HMAC access key.
         *
         * @param accessKey 访问密钥。 access key
         */
        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        /**
         * 返回 HMAC 签名密钥。 Returns the HMAC signing secret.
         *
         * @return 签名密钥。 signing secret
         */
        public String getSecretKey() {
            return secretKey;
        }

        /**
         * 设置 HMAC 签名密钥。 Sets the HMAC signing secret.
         *
         * @param secretKey 签名密钥。 signing secret
         */
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        /**
         * 返回是否启用请求签名。 Returns whether request signing is enabled.
         *
         * @return 签名启用标志。 signing enabled flag
         */
        public boolean isSignatureEnabled() {
            return signatureEnabled;
        }

        /**
         * 设置是否启用请求签名。 Sets whether request signing is enabled.
         *
         * @param signatureEnabled 签名启用标志。 signing enabled flag
         */
        public void setSignatureEnabled(boolean signatureEnabled) {
            this.signatureEnabled = signatureEnabled;
        }

        /**
         * 在启用签名时要求访问密钥和签名密钥均非空。 Requires both access and secret keys when signing is enabled.
         *
         * @throws IllegalArgumentException 启用签名但缺少任一凭据时抛出。 thrown when signing is enabled but either credential is missing
         */
        public void validateCredentials() {
            if (!signatureEnabled) {
                return;
            }
            if (accessKey == null || accessKey.isBlank()) {
                throw new IllegalArgumentException(
                        "egon.cola.component.ddc.admin.access-key "
                                + "is required when signature is enabled"
                );
            }
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalArgumentException(
                        "egon.cola.component.ddc.admin.secret-key "
                                + "is required when signature is enabled"
                );
            }
        }

        /**
         * 返回连接超时时间。 Returns the connection timeout.
         *
         * @return 连接超时时间。 connection timeout
         */
        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * 设置连接超时时间。 Sets the connection timeout.
         *
         * @param connectTimeout 连接超时时间。 connection timeout
         */
        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        /**
         * 返回读取超时时间。 Returns the read timeout.
         *
         * @return 读取超时时间。 read timeout
         */
        public Duration getReadTimeout() {
            return readTimeout;
        }

        /**
         * 设置读取超时时间。 Sets the read timeout.
         *
         * @param readTimeout 读取超时时间。 read timeout
         */
        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        /**
         * 返回 TLS 配置。 Returns the TLS settings.
         *
         * @return TLS 配置。 TLS settings
         */
        public Tls getTls() {
            return tls;
        }

        /**
         * 替换 TLS 配置。 Replaces the TLS settings.
         *
         * @param tls TLS 配置。 TLS settings
         */
        public void setTls(Tls tls) {
            this.tls = tls;
        }
    }

    /**
     * 定义管理端双向 TLS 证书路径和显式开发明文开关。 Defines management mutual-TLS certificate paths and the explicit development plaintext switch.
     */
    public static class Tls {

        /**
         * 是否启用双向 TLS。 Whether mutual TLS is enabled.
         */
        private boolean enabled;

        /**
         * 是否明确允许仅用于开发环境的明文 HTTP。 Whether plaintext HTTP is explicitly allowed for development only.
         */
        private boolean developmentPlaintext;

        /**
         * 客户端证书链文件路径。 Client certificate-chain file path.
         */
        private String certificateChainPath;

        /**
         * 客户端私钥文件路径。 Client private-key file path.
         */
        private String privateKeyPath;

        /**
         * 服务端信任证书集合文件路径。 Server trust-certificate collection file path.
         */
        private String trustCertificateCollectionPath;

        /**
         * 返回是否启用双向 TLS。 Returns whether mutual TLS is enabled.
         *
         * @return TLS 启用标志。 TLS enabled flag
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用双向 TLS。 Sets whether mutual TLS is enabled.
         *
         * @param enabled TLS 启用标志。 TLS enabled flag
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回是否允许开发明文传输。 Returns whether development plaintext transport is allowed.
         *
         * @return 开发明文标志。 development plaintext flag
         */
        public boolean isDevelopmentPlaintext() {
            return developmentPlaintext;
        }

        /**
         * 设置是否明确允许开发明文传输。 Sets whether development plaintext transport is explicitly allowed.
         *
         * @param developmentPlaintext 开发明文标志。 development plaintext flag
         */
        public void setDevelopmentPlaintext(boolean developmentPlaintext) {
            this.developmentPlaintext = developmentPlaintext;
        }

        /**
         * 返回客户端证书链路径。 Returns the client certificate-chain path.
         *
         * @return 证书链路径。 certificate-chain path
         */
        public String getCertificateChainPath() {
            return certificateChainPath;
        }

        /**
         * 设置客户端证书链路径。 Sets the client certificate-chain path.
         *
         * @param certificateChainPath 证书链路径。 certificate-chain path
         */
        public void setCertificateChainPath(String certificateChainPath) {
            this.certificateChainPath = certificateChainPath;
        }

        /**
         * 返回客户端私钥路径。 Returns the client private-key path.
         *
         * @return 私钥路径。 private-key path
         */
        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        /**
         * 设置客户端私钥路径。 Sets the client private-key path.
         *
         * @param privateKeyPath 私钥路径。 private-key path
         */
        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        /**
         * 返回服务端信任证书集合路径。 Returns the server trust-certificate collection path.
         *
         * @return 信任证书集合路径。 trust-certificate collection path
         */
        public String getTrustCertificateCollectionPath() {
            return trustCertificateCollectionPath;
        }

        /**
         * 设置服务端信任证书集合路径。 Sets the server trust-certificate collection path.
         *
         * @param trustCertificateCollectionPath 信任证书集合路径。 trust-certificate collection path
         */
        public void setTrustCertificateCollectionPath(
                String trustCertificateCollectionPath) {
            this.trustCertificateCollectionPath =
                    trustCertificateCollectionPath;
        }
    }

    /**
     * 定义 DDC 使用的 Redis 单机、哨兵或集群连接参数。 Defines standalone, sentinel, or cluster Redis connection settings used by DDC.
     */
    public static class Redis {

        /**
         * 是否创建 DDC Redis 客户端。 Whether the DDC Redis client is created.
         */
        private boolean enabled = true;

        /**
         * Redis 拓扑模式。 Redis topology mode.
         */
        private String mode = "SINGLE";

        /**
         * 单机覆盖地址、哨兵地址或集群节点地址。 Standalone override, sentinel addresses, or cluster node addresses.
         */
        private List<String> nodes = new ArrayList<>();

        /**
         * 哨兵模式监控的主节点名称。 Master name monitored in sentinel mode.
         */
        private String masterName;

        /**
         * 未提供节点地址时单机模式使用的主机。 Host used by standalone mode when no node URL is supplied.
         */
        private String host = "127.0.0.1";

        /**
         * 未提供节点地址时单机模式使用的端口。 Port used by standalone mode when no node URL is supplied.
         */
        private int port = 6379;

        /**
         * 可选 Redis 密码。 Optional Redis password.
         */
        private String password;

        /**
         * 单机或哨兵模式使用的逻辑数据库索引。 Logical database index used by standalone or sentinel mode.
         */
        private int database = 0;

        /**
         * 返回是否启用 DDC Redis 客户端。 Returns whether the DDC Redis client is enabled.
         *
         * @return Redis 客户端启用标志。 Redis client enabled flag
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 DDC Redis 客户端。 Sets whether the DDC Redis client is enabled.
         *
         * @param enabled 启用标志。 enabled flag
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回 Redis 拓扑模式。 Returns the Redis topology mode.
         *
         * @return 拓扑模式。 topology mode
         */
        public String getMode() {
            return mode;
        }

        /**
         * 设置 Redis 拓扑模式。 Sets the Redis topology mode.
         *
         * @param mode SINGLE、SENTINEL 或 CLUSTER。 SINGLE, SENTINEL, or CLUSTER
         */
        public void setMode(String mode) {
            this.mode = mode;
        }

        /**
         * 返回配置的 Redis 节点 URL。 Returns the configured Redis node URLs.
         *
         * @return 节点 URL 列表。 node URL list
         */
        public List<String> getNodes() {
            return nodes;
        }

        /**
         * 设置 Redis 节点 URL。 Sets Redis node URLs.
         *
         * @param nodes 使用 redis:// 或 rediss:// 的节点列表。 node list using redis:// or rediss://
         */
        public void setNodes(List<String> nodes) {
            this.nodes = nodes;
        }

        /**
         * 返回哨兵主节点名称。 Returns the sentinel master name.
         *
         * @return 主节点名称。 master name
         */
        public String getMasterName() {
            return masterName;
        }

        /**
         * 设置哨兵主节点名称。 Sets the sentinel master name.
         *
         * @param masterName 主节点名称。 master name
         */
        public void setMasterName(String masterName) {
            this.masterName = masterName;
        }

        /**
         * 返回单机 Redis 主机。 Returns the standalone Redis host.
         *
         * @return Redis 主机。 Redis host
         */
        public String getHost() {
            return host;
        }

        /**
         * 设置单机 Redis 主机。 Sets the standalone Redis host.
         *
         * @param host Redis 主机。 Redis host
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * 返回单机 Redis 端口。 Returns the standalone Redis port.
         *
         * @return Redis 端口。 Redis port
         */
        public int getPort() {
            return port;
        }

        /**
         * 设置单机 Redis 端口。 Sets the standalone Redis port.
         *
         * @param port Redis 端口。 Redis port
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * 返回 Redis 密码。 Returns the Redis password.
         *
         * @return Redis 密码。 Redis password
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置 Redis 密码。 Sets the Redis password.
         *
         * @param password Redis 密码。 Redis password
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 返回逻辑数据库索引。 Returns the logical database index.
         *
         * @return 数据库索引。 database index
         */
        public int getDatabase() {
            return database;
        }

        /**
         * 设置逻辑数据库索引。 Sets the logical database index.
         *
         * @param database 数据库索引。 database index
         */
        public void setDatabase(int database) {
            this.database = database;
        }
    }

    /**
     * 定义配置客户端实例标识、心跳周期和租约时长。 Defines the configuration-client instance identifier, heartbeat interval, and lease duration.
     */
    public static class Instance {

        /**
         * 可选显式实例标识。 Optional explicit instance identifier.
         */
        private String id;

        /**
         * 实例心跳发送间隔秒数。 Interval in seconds between instance heartbeats.
         */
        private int heartbeatIntervalSeconds = 10;

        /**
         * 管理端授予的实例租约时长秒数。 Instance lease duration in seconds granted by the management service.
         */
        private int leaseSeconds = 30;

        /**
         * 返回显式实例标识。 Returns the explicit instance identifier.
         *
         * @return 实例标识。 instance identifier
         */
        public String getId() {
            return id;
        }

        /**
         * 设置显式实例标识。 Sets the explicit instance identifier.
         *
         * @param id 实例标识。 instance identifier
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * 返回心跳间隔秒数。 Returns the heartbeat interval in seconds.
         *
         * @return 心跳间隔秒数。 heartbeat interval in seconds
         */
        public int getHeartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        /**
         * 设置心跳间隔秒数。 Sets the heartbeat interval in seconds.
         *
         * @param heartbeatIntervalSeconds 心跳间隔秒数。 heartbeat interval in seconds
         */
        public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }

        /**
         * 返回租约时长秒数。 Returns the lease duration in seconds.
         *
         * @return 租约时长秒数。 lease duration in seconds
         */
        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        /**
         * 设置租约时长秒数。 Sets the lease duration in seconds.
         *
         * @param leaseSeconds 租约时长秒数。 lease duration in seconds
         */
        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }

        /**
         * 校验心跳间隔为正且严格小于租约时长。 Validates that the heartbeat interval is positive and strictly shorter than the lease.
         *
         * @throws IllegalArgumentException 心跳与租约时序无效时抛出。 thrown when heartbeat and lease timing is invalid
         */
        public void validate() {
            if (heartbeatIntervalSeconds <= 0
                    || heartbeatIntervalSeconds >= leaseSeconds) {
                throw new IllegalArgumentException(
                        "egon.cola.component.ddc.instance.heartbeat-interval-seconds "
                                + "must be positive and less than lease-seconds"
                );
            }
        }

        /**
         * 设置旧版“心跳超时”属性，并将其映射到租约时长。 Sets the legacy heartbeat-timeout property by mapping it to lease duration.
         *
         * @param heartbeatTimeoutSeconds 兼容旧配置的超时秒数。 timeout seconds from legacy configuration
         * @deprecated 请改用 {@link #setLeaseSeconds(int)}。 Use {@link #setLeaseSeconds(int)} instead.
         */
        @Deprecated(forRemoval = true)
        public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
            this.leaseSeconds = heartbeatTimeoutSeconds;
        }

        /**
         * 返回映射到租约时长的旧版“心跳超时”值。 Returns the legacy heartbeat-timeout value mapped from lease duration.
         *
         * @return 兼容旧配置的超时秒数。 timeout seconds for legacy configuration
         * @deprecated 请改用 {@link #getLeaseSeconds()}。 Use {@link #getLeaseSeconds()} instead.
         */
        @Deprecated(forRemoval = true)
        public int getHeartbeatTimeoutSeconds() {
            return leaseSeconds;
        }
    }

    /**
     * 定义启动失败策略以及后台配置对账行为。 Defines startup failure policy and background configuration reconciliation behavior.
     */
    public static class Consistency {

        /**
         * 初始注册或拉取失败时是否立即阻止启动。 Whether initial registration or pull failure immediately aborts startup.
         */
        private boolean failFast = true;

        /**
         * 是否启用后台配置对账。 Whether background configuration reconciliation is enabled.
         */
        private boolean reconcileEnabled = true;

        /**
         * 后台配置对账周期秒数。 Background configuration reconciliation interval in seconds.
         */
        private int reconcileIntervalSeconds = 30;

        /**
         * 返回是否启用快速失败。 Returns whether fail-fast behavior is enabled.
         *
         * @return 快速失败标志。 fail-fast flag
         */
        public boolean isFailFast() {
            return failFast;
        }

        /**
         * 设置初始失败时是否快速失败。 Sets whether initial failures use fail-fast behavior.
         *
         * @param failFast 快速失败标志。 fail-fast flag
         */
        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }

        /**
         * 返回是否启用后台配置对账。 Returns whether background configuration reconciliation is enabled.
         *
         * @return 对账启用标志。 reconciliation enabled flag
         */
        public boolean isReconcileEnabled() {
            return reconcileEnabled;
        }

        /**
         * 设置是否启用后台配置对账。 Sets whether background configuration reconciliation is enabled.
         *
         * @param reconcileEnabled 对账启用标志。 reconciliation enabled flag
         */
        public void setReconcileEnabled(boolean reconcileEnabled) {
            this.reconcileEnabled = reconcileEnabled;
        }

        /**
         * 返回配置对账周期秒数。 Returns the configuration reconciliation interval in seconds.
         *
         * @return 对账周期秒数。 reconciliation interval in seconds
         */
        public int getReconcileIntervalSeconds() {
            return reconcileIntervalSeconds;
        }

        /**
         * 设置配置对账周期秒数。 Sets the configuration reconciliation interval in seconds.
         *
         * @param reconcileIntervalSeconds 对账周期秒数。 reconciliation interval in seconds
         */
        public void setReconcileIntervalSeconds(int reconcileIntervalSeconds) {
            this.reconcileIntervalSeconds = reconcileIntervalSeconds;
        }
    }

    /**
     * 定义服务注册客户端开关和服务目录对账周期。 Defines the service-registry client switch and service-catalog reconciliation interval.
     */
    public static class Registry {

        /**
         * 是否启用服务注册客户端。 Whether the service-registry client is enabled.
         */
        private boolean enabled;

        /**
         * 服务目录对账周期秒数。 Service-catalog reconciliation interval in seconds.
         */
        private int reconcileIntervalSeconds = 10;

        /**
         * 返回是否启用服务注册客户端。 Returns whether the service-registry client is enabled.
         *
         * @return 服务注册启用标志。 service-registry enabled flag
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用服务注册客户端。 Sets whether the service-registry client is enabled.
         *
         * @param enabled 启用标志。 enabled flag
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回服务目录对账周期秒数。 Returns the service-catalog reconciliation interval in seconds.
         *
         * @return 对账周期秒数。 reconciliation interval in seconds
         */
        public int getReconcileIntervalSeconds() {
            return reconcileIntervalSeconds;
        }

        /**
         * 设置服务目录对账周期秒数。 Sets the service-catalog reconciliation interval in seconds.
         *
         * @param reconcileIntervalSeconds 对账周期秒数。 reconciliation interval in seconds
         */
        public void setReconcileIntervalSeconds(int reconcileIntervalSeconds) {
            this.reconcileIntervalSeconds = reconcileIntervalSeconds;
        }
    }
}
