package top.egon.cola.platform.rbac3.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("egon.cola.platform.rbac3")
public class Rbac3StarterProperties {

    private boolean enabled;
    private String issuer;
    private String audience;
    private String jwkSetUri;
    private final Runtime runtime = new Runtime();
    private final Manifest manifest = new Manifest();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public Runtime getRuntime() {
        return runtime;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public static class Runtime {
        private boolean redisEnabled;
        private String redisAddress = "redis://127.0.0.1:6379";
        private int database;
        private String passwordFile;
        private Duration timeout = Duration.ofSeconds(2);

        public boolean isRedisEnabled() {
            return redisEnabled;
        }

        public void setRedisEnabled(boolean redisEnabled) {
            this.redisEnabled = redisEnabled;
        }

        public String getRedisAddress() {
            return redisAddress;
        }

        public void setRedisAddress(String redisAddress) {
            this.redisAddress = redisAddress;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public String getPasswordFile() {
            return passwordFile;
        }

        public void setPasswordFile(String passwordFile) {
            this.passwordFile = passwordFile;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class Manifest {
        private boolean reportingEnabled;
        private String serviceCredentialFile;

        public boolean isReportingEnabled() {
            return reportingEnabled;
        }

        public void setReportingEnabled(boolean reportingEnabled) {
            this.reportingEnabled = reportingEnabled;
        }

        public String getServiceCredentialFile() {
            return serviceCredentialFile;
        }

        public void setServiceCredentialFile(String serviceCredentialFile) {
            this.serviceCredentialFile = serviceCredentialFile;
        }
    }
}
