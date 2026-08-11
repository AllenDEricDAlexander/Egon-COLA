package top.egon.cola.platform.rbac3.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("egon.cola.platform.rbac3")
public class Rbac3StarterProperties {

    private boolean enabled;
    private boolean registerFilter = true;
    private String systemCode;
    private final Runtime runtime = new Runtime();
    private final Authorization authorization = new Authorization();
    private final Manifest manifest = new Manifest();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRegisterFilter() {
        return registerFilter;
    }

    public void setRegisterFilter(boolean registerFilter) {
        this.registerFilter = registerFilter;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public Authorization getAuthorization() {
        return authorization;
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

    public static class Authorization {
        private String endpoint;
        private String serviceCredentialFile;
        private Duration cacheTtl = Duration.ofMinutes(5);
        private Duration maximumJitter = Duration.ofSeconds(30);
        private Duration nearCacheTtl = Duration.ofSeconds(5);
        private Duration fetchTimeout = Duration.ofSeconds(1);
        private final ServiceToken serviceToken = new ServiceToken();

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getServiceCredentialFile() {
            return serviceCredentialFile;
        }

        public void setServiceCredentialFile(String serviceCredentialFile) {
            this.serviceCredentialFile = serviceCredentialFile;
        }

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }

        public Duration getMaximumJitter() {
            return maximumJitter;
        }

        public void setMaximumJitter(Duration maximumJitter) {
            this.maximumJitter = maximumJitter;
        }

        public Duration getNearCacheTtl() {
            return nearCacheTtl;
        }

        public void setNearCacheTtl(Duration nearCacheTtl) {
            this.nearCacheTtl = nearCacheTtl;
        }

        public Duration getFetchTimeout() {
            return fetchTimeout;
        }

        public void setFetchTimeout(Duration fetchTimeout) {
            this.fetchTimeout = fetchTimeout;
        }

        public ServiceToken getServiceToken() {
            return serviceToken;
        }

        public static class ServiceToken {
            private boolean enabled;
            private String tokenEndpoint;
            private String clientId;
            private String keyId;
            private String privateKeyFile;
            private String resourceUri;
            private String scopes;
            private Duration renewalSkew = Duration.ofSeconds(30);

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getTokenEndpoint() {
                return tokenEndpoint;
            }

            public void setTokenEndpoint(String tokenEndpoint) {
                this.tokenEndpoint = tokenEndpoint;
            }

            public String getClientId() {
                return clientId;
            }

            public void setClientId(String clientId) {
                this.clientId = clientId;
            }

            public String getKeyId() {
                return keyId;
            }

            public void setKeyId(String keyId) {
                this.keyId = keyId;
            }

            public String getPrivateKeyFile() {
                return privateKeyFile;
            }

            public void setPrivateKeyFile(String privateKeyFile) {
                this.privateKeyFile = privateKeyFile;
            }

            public String getResourceUri() {
                return resourceUri;
            }

            public void setResourceUri(String resourceUri) {
                this.resourceUri = resourceUri;
            }

            public String getScopes() {
                return scopes;
            }

            public void setScopes(String scopes) {
                this.scopes = scopes;
            }

            public Duration getRenewalSkew() {
                return renewalSkew;
            }

            public void setRenewalSkew(Duration renewalSkew) {
                this.renewalSkew = renewalSkew;
            }
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
