package top.egon.cola.component.ddc.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "egon.cola.component.ddc.admin")
public class DdcAdminProperties {

    private long maxConfigBytes = 1024L * 1024L;

    private Redis redis = new Redis();

    private Lease lease = new Lease();

    private Rpc rpc = new Rpc();

    private Security security = new Security();

    private Publish publish = new Publish();

    /** DDC registration credential verification settings. */
    private Registration registration = new Registration();

    public long getMaxConfigBytes() {
        return maxConfigBytes;
    }

    public void setMaxConfigBytes(long maxConfigBytes) {
        this.maxConfigBytes = maxConfigBytes;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Lease getLease() {
        return lease;
    }

    public void setLease(Lease lease) {
        this.lease = lease;
    }

    public Rpc getRpc() {
        return rpc;
    }

    public void setRpc(Rpc rpc) {
        this.rpc = rpc;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Publish getPublish() {
        return publish;
    }

    public void setPublish(Publish publish) {
        this.publish = publish;
    }

    /**
     * 返回 DDC registration credential verification settings。
     *
     * <p>Returns DDC registration credential verification settings.</p>
     *
     * @return registration 配置；registration settings
     */
    public Registration getRegistration() {
        return registration;
    }

    /**
     * 设置 DDC registration credential verification settings。
     *
     * <p>Sets DDC registration credential verification settings.</p>
     *
     * @param registration registration 配置；registration settings
     */
    public void setRegistration(Registration registration) {
        this.registration = registration;
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

    public static class Lease {

        private int minimumSeconds = 5;

        private int maximumSeconds = 300;

        private long scanIntervalMillis = 5000;

        public int getMinimumSeconds() {
            return minimumSeconds;
        }

        public void setMinimumSeconds(int minimumSeconds) {
            this.minimumSeconds = minimumSeconds;
        }

        public int getMaximumSeconds() {
            return maximumSeconds;
        }

        public void setMaximumSeconds(int maximumSeconds) {
            this.maximumSeconds = maximumSeconds;
        }

        public long getScanIntervalMillis() {
            return scanIntervalMillis;
        }

        public void setScanIntervalMillis(long scanIntervalMillis) {
            this.scanIntervalMillis = scanIntervalMillis;
        }
    }

    public static class Rpc {

        private boolean signatureEnabled;

        private int allowedClockSkewSeconds = 300;

        private int nonceCacheMaxSize = 10000;

        private List<Credential> credentials = new ArrayList<>();

        public boolean isSignatureEnabled() {
            return signatureEnabled;
        }

        public void setSignatureEnabled(boolean signatureEnabled) {
            this.signatureEnabled = signatureEnabled;
        }

        public int getAllowedClockSkewSeconds() {
            return allowedClockSkewSeconds;
        }

        public void setAllowedClockSkewSeconds(int allowedClockSkewSeconds) {
            this.allowedClockSkewSeconds = allowedClockSkewSeconds;
        }

        public int getNonceCacheMaxSize() {
            return nonceCacheMaxSize;
        }

        public void setNonceCacheMaxSize(int nonceCacheMaxSize) {
            this.nonceCacheMaxSize = nonceCacheMaxSize;
        }

        public List<Credential> getCredentials() {
            return credentials;
        }

        public void setCredentials(List<Credential> credentials) {
            this.credentials = credentials;
        }
    }

    public static class Credential {

        private String credentialId;

        private String accessKey;

        private String secret;

        private String clientType;

        private List<String> appCodePatterns = new ArrayList<>();

        private List<String> envPatterns = new ArrayList<>();

        private List<String> bizCodePatterns = new ArrayList<>();

        private List<String> namespacePatterns = new ArrayList<>();

        private List<String> allowedOperations = new ArrayList<>();

        public String getCredentialId() {
            return credentialId;
        }

        public void setCredentialId(String credentialId) {
            this.credentialId = credentialId;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getClientType() {
            return clientType;
        }

        public void setClientType(String clientType) {
            this.clientType = clientType;
        }

        public List<String> getAppCodePatterns() {
            return appCodePatterns;
        }

        public void setAppCodePatterns(List<String> appCodePatterns) {
            this.appCodePatterns = appCodePatterns;
        }

        public List<String> getEnvPatterns() {
            return envPatterns;
        }

        public void setEnvPatterns(List<String> envPatterns) {
            this.envPatterns = envPatterns;
        }

        public List<String> getBizCodePatterns() {
            return bizCodePatterns;
        }

        public void setBizCodePatterns(List<String> bizCodePatterns) {
            this.bizCodePatterns = bizCodePatterns;
        }

        public List<String> getNamespacePatterns() {
            return namespacePatterns;
        }

        public void setNamespacePatterns(List<String> namespacePatterns) {
            this.namespacePatterns = namespacePatterns;
        }

        public List<String> getAllowedOperations() {
            return allowedOperations;
        }

        public void setAllowedOperations(List<String> allowedOperations) {
            this.allowedOperations = allowedOperations;
        }
    }

    public static class Security {

        private boolean localDev;

        private Jwt jwt = new Jwt();

        public boolean isLocalDev() {
            return localDev;
        }

        public void setLocalDev(boolean localDev) {
            this.localDev = localDev;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public void setJwt(Jwt jwt) {
            this.jwt = jwt;
        }
    }

    public static class Jwt {

        private String issuer;

        private String audience;

        private String jwkSetUri;

        private String hmacSecretBase64;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getHmacSecretBase64() {
            return hmacSecretBase64;
        }

        public void setHmacSecretBase64(String hmacSecretBase64) {
            this.hmacSecretBase64 = hmacSecretBase64;
        }
    }

    /**
     * IdP PLATFORM SERVICE registration verification settings。
     *
     * <p>IdP PLATFORM SERVICE registration verification settings.</p>
     */
    public static class Registration {

        /** DDC Resource Server stable identifier. */
        private String resourceServerId;

        /** DDC Resource URI used as the token audience. */
        private URI resourceUri;

        /** Least-privilege registration scope. */
        private String requiredScope = "ddc:registration:write";

        /**
         * 返回 DDC Resource Server 稳定标识。
         *
         * <p>Returns the DDC Resource Server stable identifier.</p>
         *
         * @return Resource Server stable identifier
         */
        public String getResourceServerId() {
            return resourceServerId;
        }

        /**
         * 设置 DDC Resource Server 稳定标识。
         *
         * <p>Sets the DDC Resource Server stable identifier.</p>
         *
         * @param resourceServerId Resource Server stable identifier
         */
        public void setResourceServerId(String resourceServerId) {
            this.resourceServerId = resourceServerId;
        }

        /**
         * 返回 DDC Resource URI。
         *
         * <p>Returns the DDC Resource URI.</p>
         *
         * @return Resource URI
         */
        public URI getResourceUri() {
            return resourceUri;
        }

        /**
         * 设置 DDC Resource URI。
         *
         * <p>Sets the DDC Resource URI.</p>
         *
         * @param resourceUri Resource URI
         */
        public void setResourceUri(URI resourceUri) {
            this.resourceUri = resourceUri;
        }

        /**
         * 返回 DDC registration scope。
         *
         * <p>Returns the least-privilege DDC registration scope.</p>
         *
         * @return registration scope
         */
        public String getRequiredScope() {
            return requiredScope;
        }

        /**
         * 设置 DDC registration scope。
         *
         * <p>Sets the least-privilege DDC registration scope.</p>
         *
         * @param requiredScope registration scope
         */
        public void setRequiredScope(String requiredScope) {
            this.requiredScope = requiredScope;
        }
    }

    public static class Publish {

        private long dispatchTimeoutMs = 5000;

        private long defaultTimeoutMs = 30000;

        private long maxTimeoutMs = 60000;

        private long scanIntervalMs = 1000;

        private long completionPollIntervalMs = 100;

        private long recoveryStaleMs = 120000;

        public long getDispatchTimeoutMs() {
            return dispatchTimeoutMs;
        }

        public void setDispatchTimeoutMs(long dispatchTimeoutMs) {
            this.dispatchTimeoutMs = dispatchTimeoutMs;
        }

        public long getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }

        public void setDefaultTimeoutMs(long defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }

        public long getMaxTimeoutMs() {
            return maxTimeoutMs;
        }

        public void setMaxTimeoutMs(long maxTimeoutMs) {
            this.maxTimeoutMs = maxTimeoutMs;
        }

        public long getScanIntervalMs() {
            return scanIntervalMs;
        }

        public void setScanIntervalMs(long scanIntervalMs) {
            this.scanIntervalMs = scanIntervalMs;
        }

        public long getCompletionPollIntervalMs() {
            return completionPollIntervalMs;
        }

        public void setCompletionPollIntervalMs(
                long completionPollIntervalMs) {
            this.completionPollIntervalMs = completionPollIntervalMs;
        }

        public long getRecoveryStaleMs() {
            return recoveryStaleMs;
        }

        public void setRecoveryStaleMs(long recoveryStaleMs) {
            this.recoveryStaleMs = recoveryStaleMs;
        }
    }
}
