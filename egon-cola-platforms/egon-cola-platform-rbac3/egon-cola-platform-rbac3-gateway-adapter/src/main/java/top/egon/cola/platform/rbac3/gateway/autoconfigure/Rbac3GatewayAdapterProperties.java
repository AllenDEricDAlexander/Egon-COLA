package top.egon.cola.platform.rbac3.gateway.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("egon.cola.platform.rbac3.gateway")
public class Rbac3GatewayAdapterProperties {

    private boolean enabled;
    private String issuer;
    private String audience;
    private Duration clockSkew = Duration.ofMinutes(2);
    private Duration publicKeyLkgTtl = Duration.ofMinutes(5);
    private final Runtime runtime = new Runtime();

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

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public Duration getPublicKeyLkgTtl() {
        return publicKeyLkgTtl;
    }

    public void setPublicKeyLkgTtl(Duration publicKeyLkgTtl) {
        this.publicKeyLkgTtl = publicKeyLkgTtl;
    }

    public Runtime getRuntime() {
        return runtime;
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
}
