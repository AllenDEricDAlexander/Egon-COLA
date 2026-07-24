package top.egon.cola.component.ddc.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "egon.cola.component.ddc.admin", ignoreInvalidFields = true)
public class DdcAdminProperties {

    private Redis redis = new Redis();

    private Lease lease = new Lease();

    private Openapi openapi = new Openapi();

    private Manifest manifest = new Manifest();

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

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    public static class Redis {

        private boolean enabled = true;

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

        private String version = "5.2.1";

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
    }
}
