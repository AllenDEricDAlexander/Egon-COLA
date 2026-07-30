package top.egon.cola.component.ddc.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "egon.cola.component.ddc.admin")
public class DdcAdminProperties {

    private long maxValueBytes = 1024L * 1024L;

    private Redis redis = new Redis();

    private Lease lease = new Lease();

    private Openapi openapi = new Openapi();

    private Security security = new Security();

    private Publish publish = new Publish();

    private Manifest manifest = new Manifest();

    public long getMaxValueBytes() {
        return maxValueBytes;
    }

    public void setMaxValueBytes(long maxValueBytes) {
        this.maxValueBytes = maxValueBytes;
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

    public Openapi getOpenapi() {
        return openapi;
    }

    public void setOpenapi(Openapi openapi) {
        this.openapi = openapi;
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

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
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

    public static class Manifest {

        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class Openapi {

        private boolean signatureEnabled;

        private String accessKey;

        private String secretKey;

        private int allowedClockSkewSeconds = 300;

        private int nonceCacheMaxSize = 10000;

        private List<Credential> credentials = new ArrayList<>();

        public boolean isSignatureEnabled() {
            return signatureEnabled;
        }

        public void setSignatureEnabled(boolean signatureEnabled) {
            this.signatureEnabled = signatureEnabled;
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
