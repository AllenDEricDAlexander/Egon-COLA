package top.egon.cola.component.rpc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "egon.cola.component.rpc")
public class EgonRpcProperties {

    private boolean enabled;

    private Identity identity = new Identity();

    private Provider provider = new Provider();

    private Consumer consumer = new Consumer();

    private Tls tls = new Tls();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public Tls getTls() {
        return tls;
    }

    public void setTls(Tls tls) {
        this.tls = tls;
    }

    public static class Provider {

        private boolean enabled;

        private String bindAddress = "0.0.0.0";

        private int port = 19090;

        private String advertisedHost;

        private Integer advertisedPort;

        private boolean registrationFailFast = true;

        private RpcProviderRegistrationMode registrationMode =
                RpcProviderRegistrationMode.REQUIRED;

        private int leaseSeconds = 30;

        private int heartbeatIntervalSeconds = 10;

        private long gracefulShutdownTimeoutMs = 10000;

        private Map<String, String> metadata = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBindAddress() {
            return bindAddress;
        }

        public void setBindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getAdvertisedHost() {
            return advertisedHost;
        }

        public void setAdvertisedHost(String advertisedHost) {
            this.advertisedHost = advertisedHost;
        }

        public Integer getAdvertisedPort() {
            return advertisedPort;
        }

        public void setAdvertisedPort(Integer advertisedPort) {
            this.advertisedPort = advertisedPort;
        }

        public boolean isRegistrationFailFast() {
            return registrationFailFast;
        }

        public void setRegistrationFailFast(boolean registrationFailFast) {
            this.registrationFailFast = registrationFailFast;
        }

        public RpcProviderRegistrationMode getRegistrationMode() {
            return registrationMode;
        }

        public void setRegistrationMode(
                RpcProviderRegistrationMode registrationMode) {
            this.registrationMode = registrationMode;
        }

        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }

        public int getHeartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }

        public long getGracefulShutdownTimeoutMs() {
            return gracefulShutdownTimeoutMs;
        }

        public void setGracefulShutdownTimeoutMs(long gracefulShutdownTimeoutMs) {
            this.gracefulShutdownTimeoutMs = gracefulShutdownTimeoutMs;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }

    public static class Identity {

        private String env = "default";

        private String host;

        private String instanceId;

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }
    }

    public static class Consumer {

        private boolean enabled;

        private long defaultTimeoutMs = 3000;

        private long gatewayDiscoveryTimeoutMs = 5000;

        private String gatewayServiceName = "egon-gateway-rpc";

        private String gatewayGroup = "default";

        private String gatewayVersion = "1.0.0";

        private String gatewayBizCode;

        private String gatewayAppCode;

        private long channelDrainTimeoutMs = 5000;

        private int gatewayMaxAttempts = 2;

        private int maxRetries = 3;

        private LoadBalance defaultLoadBalance = LoadBalance.ROUND_ROBIN;

        private int consistentHashVirtualNodes = 160;

        private int genericCacheMaxEntries = 256;

        private long genericCacheIdleTimeoutMs = 600_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }

        public void setDefaultTimeoutMs(long defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }

        public long getGatewayDiscoveryTimeoutMs() {
            return gatewayDiscoveryTimeoutMs;
        }

        public void setGatewayDiscoveryTimeoutMs(long gatewayDiscoveryTimeoutMs) {
            this.gatewayDiscoveryTimeoutMs = gatewayDiscoveryTimeoutMs;
        }

        public String getGatewayServiceName() {
            return gatewayServiceName;
        }

        public void setGatewayServiceName(String gatewayServiceName) {
            this.gatewayServiceName = gatewayServiceName;
        }

        public String getGatewayGroup() {
            return gatewayGroup;
        }

        public void setGatewayGroup(String gatewayGroup) {
            this.gatewayGroup = gatewayGroup;
        }

        public String getGatewayVersion() {
            return gatewayVersion;
        }

        public void setGatewayVersion(String gatewayVersion) {
            this.gatewayVersion = gatewayVersion;
        }

        public String getGatewayBizCode() {
            return gatewayBizCode;
        }

        public void setGatewayBizCode(String gatewayBizCode) {
            this.gatewayBizCode = gatewayBizCode;
        }

        public String getGatewayAppCode() {
            return gatewayAppCode;
        }

        public void setGatewayAppCode(String gatewayAppCode) {
            this.gatewayAppCode = gatewayAppCode;
        }

        public long getChannelDrainTimeoutMs() {
            return channelDrainTimeoutMs;
        }

        public void setChannelDrainTimeoutMs(long channelDrainTimeoutMs) {
            this.channelDrainTimeoutMs = channelDrainTimeoutMs;
        }

        public int getGatewayMaxAttempts() {
            return gatewayMaxAttempts;
        }

        public void setGatewayMaxAttempts(int gatewayMaxAttempts) {
            this.gatewayMaxAttempts = gatewayMaxAttempts;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public LoadBalance getDefaultLoadBalance() {
            return defaultLoadBalance;
        }

        public void setDefaultLoadBalance(LoadBalance defaultLoadBalance) {
            this.defaultLoadBalance = defaultLoadBalance;
        }

        public int getConsistentHashVirtualNodes() {
            return consistentHashVirtualNodes;
        }

        public void setConsistentHashVirtualNodes(int consistentHashVirtualNodes) {
            this.consistentHashVirtualNodes = consistentHashVirtualNodes;
        }

        public int getGenericCacheMaxEntries() {
            return genericCacheMaxEntries;
        }

        public void setGenericCacheMaxEntries(int genericCacheMaxEntries) {
            this.genericCacheMaxEntries = genericCacheMaxEntries;
        }

        public long getGenericCacheIdleTimeoutMs() {
            return genericCacheIdleTimeoutMs;
        }

        public void setGenericCacheIdleTimeoutMs(long genericCacheIdleTimeoutMs) {
            this.genericCacheIdleTimeoutMs = genericCacheIdleTimeoutMs;
        }

        public void validateSharedSettings() {
            if (defaultTimeoutMs <= 0 || channelDrainTimeoutMs <= 0
                    || gatewayMaxAttempts < 1 || gatewayMaxAttempts > 10
                    || maxRetries < 0 || maxRetries > 10
                    || defaultLoadBalance == null
                    || defaultLoadBalance == LoadBalance.INHERIT
                    || consistentHashVirtualNodes < 16
                    || consistentHashVirtualNodes > 4096
                    || genericCacheMaxEntries < 1
                    || genericCacheMaxEntries > 4096
                    || genericCacheIdleTimeoutMs < 1_000
                    || genericCacheIdleTimeoutMs > 86_400_000) {
                throw new EgonRpcException(
                        EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                        "RPC Consumer settings are outside supported bounds"
                );
            }
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
}
