package top.egon.cola.component.ddc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "egon.cola.component.ddc", ignoreInvalidFields = true)
public class DdcProperties {

    private boolean enabled;

    private String appCode = "default-app";

    private String bizCode;

    private String env = "dev";

    private String namespace = "default";

    private long maxYamlBytes = 1024L * 1024L;

    private Admin admin = new Admin();

    private Redis redis = new Redis();

    private Instance instance = new Instance();

    private Registry registry = new Registry();

    private Consistency consistency = new Consistency();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
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

    public long getMaxYamlBytes() {
        return maxYamlBytes;
    }

    public void setMaxYamlBytes(long maxYamlBytes) {
        this.maxYamlBytes = maxYamlBytes;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Consistency getConsistency() {
        return consistency;
    }

    public void setConsistency(Consistency consistency) {
        this.consistency = consistency;
    }

    public static class Admin {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private boolean signatureEnabled;

        private Duration connectTimeout = Duration.ofSeconds(3);

        private Duration readTimeout = Duration.ofSeconds(10);

        private Tls tls = new Tls();

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

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

        private IllegalArgumentException invalidEndpoint(Throwable cause) {
            String message =
                    "egon.cola.component.ddc.admin.endpoint "
                            + "must be an HTTP or HTTPS root URI";
            return cause == null
                    ? new IllegalArgumentException(message)
                    : new IllegalArgumentException(message, cause);
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

        public boolean isSignatureEnabled() {
            return signatureEnabled;
        }

        public void setSignatureEnabled(boolean signatureEnabled) {
            this.signatureEnabled = signatureEnabled;
        }

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

        public Tls getTls() {
            return tls;
        }

        public void setTls(Tls tls) {
            this.tls = tls;
        }
    }

    public static class Tls {

        private boolean enabled;

        private boolean developmentPlaintext;

        private String certificateChainPath;

        private String privateKeyPath;

        private String trustCertificateCollectionPath;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDevelopmentPlaintext() {
            return developmentPlaintext;
        }

        public void setDevelopmentPlaintext(boolean developmentPlaintext) {
            this.developmentPlaintext = developmentPlaintext;
        }

        public String getCertificateChainPath() {
            return certificateChainPath;
        }

        public void setCertificateChainPath(String certificateChainPath) {
            this.certificateChainPath = certificateChainPath;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getTrustCertificateCollectionPath() {
            return trustCertificateCollectionPath;
        }

        public void setTrustCertificateCollectionPath(
                String trustCertificateCollectionPath) {
            this.trustCertificateCollectionPath =
                    trustCertificateCollectionPath;
        }
    }

    public static class Redis {

        private boolean enabled = true;

        private String mode = "SINGLE";

        private List<String> nodes = new ArrayList<>();

        private String masterName;

        private String host = "127.0.0.1";

        private int port = 6379;

        private String password;

        private int database = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public List<String> getNodes() {
            return nodes;
        }

        public void setNodes(List<String> nodes) {
            this.nodes = nodes;
        }

        public String getMasterName() {
            return masterName;
        }

        public void setMasterName(String masterName) {
            this.masterName = masterName;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }

    public static class Instance {

        private String id;

        private int heartbeatIntervalSeconds = 10;

        private int leaseSeconds = 30;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getHeartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }

        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }

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
         * @deprecated use {@link #setLeaseSeconds(int)}.
         */
        @Deprecated(forRemoval = true)
        public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
            this.leaseSeconds = heartbeatTimeoutSeconds;
        }

        /**
         * @deprecated use {@link #getLeaseSeconds()}.
         */
        @Deprecated(forRemoval = true)
        public int getHeartbeatTimeoutSeconds() {
            return leaseSeconds;
        }
    }

    public static class Consistency {

        private boolean failFast = true;

        private boolean reconcileEnabled = true;

        private int reconcileIntervalSeconds = 30;

        public boolean isFailFast() {
            return failFast;
        }

        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }

        public boolean isReconcileEnabled() {
            return reconcileEnabled;
        }

        public void setReconcileEnabled(boolean reconcileEnabled) {
            this.reconcileEnabled = reconcileEnabled;
        }

        public int getReconcileIntervalSeconds() {
            return reconcileIntervalSeconds;
        }

        public void setReconcileIntervalSeconds(int reconcileIntervalSeconds) {
            this.reconcileIntervalSeconds = reconcileIntervalSeconds;
        }
    }

    public static class Registry {

        private boolean enabled;

        private int reconcileIntervalSeconds = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getReconcileIntervalSeconds() {
            return reconcileIntervalSeconds;
        }

        public void setReconcileIntervalSeconds(int reconcileIntervalSeconds) {
            this.reconcileIntervalSeconds = reconcileIntervalSeconds;
        }
    }
}
