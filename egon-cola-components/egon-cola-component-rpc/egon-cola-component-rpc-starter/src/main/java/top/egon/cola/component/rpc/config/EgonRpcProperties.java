package top.egon.cola.component.rpc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "egon.cola.component.rpc")
public class EgonRpcProperties {

    private boolean enabled;

    private Provider provider = new Provider();

    private Consumer consumer = new Consumer();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public static class Provider {

        private boolean enabled;

        private String bindAddress = "0.0.0.0";

        private int port = 19090;

        private String advertisedHost;

        private Integer advertisedPort;

        private boolean registrationFailFast = true;

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

    public static class Consumer {

        private boolean enabled;

        private long defaultTimeoutMs = 3000;

        private long gatewayDiscoveryTimeoutMs = 5000;

        private String gatewayServiceName = "egon-internal-rpc-gateway";

        private String gatewayGroup = "default";

        private String gatewayVersion = "1.0.0";

        private long channelDrainTimeoutMs = 5000;

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

        public long getChannelDrainTimeoutMs() {
            return channelDrainTimeoutMs;
        }

        public void setChannelDrainTimeoutMs(long channelDrainTimeoutMs) {
            this.channelDrainTimeoutMs = channelDrainTimeoutMs;
        }
    }
}
