package top.egon.cola.component.ddc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "egon.cola.component.ddc", ignoreInvalidFields = true)
public class DdcProperties {

    private boolean enabled = true;

    private String appCode = "default-app";

    private String env = "dev";

    private String namespace = "default";

    private Admin admin = new Admin();

    private Redis redis = new Redis();

    private Instance instance = new Instance();

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

    public Consistency getConsistency() {
        return consistency;
    }

    public void setConsistency(Consistency consistency) {
        this.consistency = consistency;
    }

    public static class Admin {

        private String endpoint = "http://localhost:18080";

        private String accessKey;

        private String secretKey;

        private boolean signatureEnabled;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
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

    public static class Instance {

        private int heartbeatIntervalSeconds = 10;

        private int heartbeatTimeoutSeconds = 30;

        public int getHeartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }

        public int getHeartbeatTimeoutSeconds() {
            return heartbeatTimeoutSeconds;
        }

        public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
            this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        }
    }

    public static class Consistency {

        private boolean ackEnabled = true;

        private boolean failFast = true;

        public boolean isAckEnabled() {
            return ackEnabled;
        }

        public void setAckEnabled(boolean ackEnabled) {
            this.ackEnabled = ackEnabled;
        }

        public boolean isFailFast() {
            return failFast;
        }

        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }
    }
}
