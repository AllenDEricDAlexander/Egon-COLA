package top.egon.cola.platform.rbac3.gateway.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the RBAC3 Gateway authorization adapter.
 *
 * <p>The adapter no longer verifies access tokens or owns identity/session
 * configuration. Token verification and refresh belong to the IdP Gateway
 * adapter. RBAC3 only needs its Redis runtime projection connection.</p>
 */
@ConfigurationProperties("egon.cola.platform.rbac3.gateway")
public class Rbac3GatewayAdapterProperties {

    private boolean enabled;
    private final Runtime runtime = new Runtime();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    /**
     * Redis connection settings for the published RBAC3 runtime projection.
     */
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
