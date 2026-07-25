package top.egon.cola.component.gateway.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "egon.cola.component.gateway.engine")
public class GatewayEngineRuntimeProperties {

    private String gatewayGroupCode = "default";

    private String env = "dev";

    private String namespace = "default";

    private String nodeId = "gateway-engine";

    private String instanceId = "gateway-engine";

    private String dataDirectory = "./data/gateway-engine";

    private Http http = new Http();

    private Rpc rpc = new Rpc();

    private Kafka kafka = new Kafka();

    private Security security = new Security();

    private ActiveHealth activeHealth = new ActiveHealth();

    public String getGatewayGroupCode() {
        return gatewayGroupCode;
    }

    public void setGatewayGroupCode(String gatewayGroupCode) {
        this.gatewayGroupCode = gatewayGroupCode;
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public Rpc getRpc() {
        return rpc;
    }

    public void setRpc(Rpc rpc) {
        this.rpc = rpc;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public ActiveHealth getActiveHealth() {
        return activeHealth;
    }

    public void setActiveHealth(ActiveHealth activeHealth) {
        this.activeHealth = activeHealth;
    }

    public static class Http {

        private boolean publicEnabled = true;

        private String publicHost = "0.0.0.0";

        private int publicPort = 18081;

        private boolean internalEnabled = true;

        private String internalHost = "0.0.0.0";

        private int internalPort = 18082;

        private int maxHeaderCount = 128;

        private int maxHeaderBytes = 64 * 1024;

        private long maxBodyBytes = 2L * 1024 * 1024;

        private Duration idleTimeout = Duration.ofSeconds(30);

        private Duration upstreamTimeout = Duration.ofSeconds(5);

        private Duration drainTimeout = Duration.ofSeconds(10);

        private int upstreamMaxConnections = 512;

        private int upstreamPendingAcquireMaxCount = 1024;

        private Tls publicTls = new Tls();

        private Tls internalTls = new Tls();

        public boolean isPublicEnabled() {
            return publicEnabled;
        }

        public void setPublicEnabled(boolean publicEnabled) {
            this.publicEnabled = publicEnabled;
        }

        public String getPublicHost() {
            return publicHost;
        }

        public void setPublicHost(String publicHost) {
            this.publicHost = publicHost;
        }

        public int getPublicPort() {
            return publicPort;
        }

        public void setPublicPort(int publicPort) {
            this.publicPort = publicPort;
        }

        public boolean isInternalEnabled() {
            return internalEnabled;
        }

        public void setInternalEnabled(boolean internalEnabled) {
            this.internalEnabled = internalEnabled;
        }

        public String getInternalHost() {
            return internalHost;
        }

        public void setInternalHost(String internalHost) {
            this.internalHost = internalHost;
        }

        public int getInternalPort() {
            return internalPort;
        }

        public void setInternalPort(int internalPort) {
            this.internalPort = internalPort;
        }

        public int getMaxHeaderCount() {
            return maxHeaderCount;
        }

        public void setMaxHeaderCount(int maxHeaderCount) {
            this.maxHeaderCount = maxHeaderCount;
        }

        public int getMaxHeaderBytes() {
            return maxHeaderBytes;
        }

        public void setMaxHeaderBytes(int maxHeaderBytes) {
            this.maxHeaderBytes = maxHeaderBytes;
        }

        public long getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(long maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        public Duration getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public Duration getUpstreamTimeout() {
            return upstreamTimeout;
        }

        public void setUpstreamTimeout(Duration upstreamTimeout) {
            this.upstreamTimeout = upstreamTimeout;
        }

        public Duration getDrainTimeout() {
            return drainTimeout;
        }

        public void setDrainTimeout(Duration drainTimeout) {
            this.drainTimeout = drainTimeout;
        }

        public int getUpstreamMaxConnections() {
            return upstreamMaxConnections;
        }

        public void setUpstreamMaxConnections(int upstreamMaxConnections) {
            this.upstreamMaxConnections = upstreamMaxConnections;
        }

        public int getUpstreamPendingAcquireMaxCount() {
            return upstreamPendingAcquireMaxCount;
        }

        public void setUpstreamPendingAcquireMaxCount(
                int upstreamPendingAcquireMaxCount) {
            this.upstreamPendingAcquireMaxCount =
                    upstreamPendingAcquireMaxCount;
        }

        public Tls getPublicTls() {
            return publicTls;
        }

        public void setPublicTls(Tls publicTls) {
            this.publicTls = publicTls;
        }

        public Tls getInternalTls() {
            return internalTls;
        }

        public void setInternalTls(Tls internalTls) {
            this.internalTls = internalTls;
        }
    }

    public static class Rpc {

        private boolean enabled = true;

        private int port = 19090;

        private String advertisedHost = "127.0.0.1";

        private String serviceName = "egon-gateway-rpc";

        private String group = "default";

        private String version = "1.0.0";

        private int maxInboundMessageBytes = 4 * 1024 * 1024;

        private Duration maximumTimeout = Duration.ofSeconds(10);

        private Duration channelDrainTimeout = Duration.ofSeconds(5);

        private int leaseSeconds = 30;

        private int heartbeatIntervalSeconds = 10;

        private Tls tls = new Tls();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public int getMaxInboundMessageBytes() {
            return maxInboundMessageBytes;
        }

        public void setMaxInboundMessageBytes(int maxInboundMessageBytes) {
            this.maxInboundMessageBytes = maxInboundMessageBytes;
        }

        public Duration getMaximumTimeout() {
            return maximumTimeout;
        }

        public void setMaximumTimeout(Duration maximumTimeout) {
            this.maximumTimeout = maximumTimeout;
        }

        public Duration getChannelDrainTimeout() {
            return channelDrainTimeout;
        }

        public void setChannelDrainTimeout(Duration channelDrainTimeout) {
            this.channelDrainTimeout = channelDrainTimeout;
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

        public void setHeartbeatIntervalSeconds(
                int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }

        public Tls getTls() {
            return tls;
        }

        public void setTls(Tls tls) {
            this.tls = tls;
        }
    }

    public static class Tls {

        private boolean enabled;

        private boolean developmentPlaintext;

        private String certificateChainPath;

        private String privateKeyPath;

        private String trustCertificateCollectionPath;

        private boolean clientCertificateRequired;

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

        public boolean isClientCertificateRequired() {
            return clientCertificateRequired;
        }

        public void setClientCertificateRequired(
                boolean clientCertificateRequired) {
            this.clientCertificateRequired = clientCertificateRequired;
        }
    }

    public static class ActiveHealth {

        private boolean enabled;

        private Duration interval = Duration.ofSeconds(10);

        private double jitterRatio = 0.2;

        private Duration timeout = Duration.ofSeconds(2);

        private int maximumConcurrency = 16;

        private int failureThreshold = 2;

        private int successThreshold = 2;

        private String httpMethod = "GET";

        private String httpPath = "/actuator/health";

        private List<Integer> httpSuccessStatuses = List.of(200);

        private String rpcServiceName = "";

        private boolean rpcConnectFallback = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public double getJitterRatio() {
            return jitterRatio;
        }

        public void setJitterRatio(double jitterRatio) {
            this.jitterRatio = jitterRatio;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaximumConcurrency() {
            return maximumConcurrency;
        }

        public void setMaximumConcurrency(int maximumConcurrency) {
            this.maximumConcurrency = maximumConcurrency;
        }

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public int getSuccessThreshold() {
            return successThreshold;
        }

        public void setSuccessThreshold(int successThreshold) {
            this.successThreshold = successThreshold;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getHttpPath() {
            return httpPath;
        }

        public void setHttpPath(String httpPath) {
            this.httpPath = httpPath;
        }

        public List<Integer> getHttpSuccessStatuses() {
            return httpSuccessStatuses;
        }

        public void setHttpSuccessStatuses(
                List<Integer> httpSuccessStatuses) {
            this.httpSuccessStatuses = httpSuccessStatuses;
        }

        public String getRpcServiceName() {
            return rpcServiceName;
        }

        public void setRpcServiceName(String rpcServiceName) {
            this.rpcServiceName = rpcServiceName;
        }

        public boolean isRpcConnectFallback() {
            return rpcConnectFallback;
        }

        public void setRpcConnectFallback(
                boolean rpcConnectFallback) {
            this.rpcConnectFallback = rpcConnectFallback;
        }
    }

    public static class Kafka {

        private boolean enabled;

        private String bootstrapServers = "127.0.0.1:9092";

        private String topic = "egon.gateway.call.v1";

        private int maxQueuedEvents = 8192;

        private long maxQueuedBytes = 32L * 1024 * 1024;

        private Duration deliveryTimeout = Duration.ofSeconds(10);

        private Duration shutdownDrain = Duration.ofSeconds(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public int getMaxQueuedEvents() {
            return maxQueuedEvents;
        }

        public void setMaxQueuedEvents(int maxQueuedEvents) {
            this.maxQueuedEvents = maxQueuedEvents;
        }

        public long getMaxQueuedBytes() {
            return maxQueuedBytes;
        }

        public void setMaxQueuedBytes(long maxQueuedBytes) {
            this.maxQueuedBytes = maxQueuedBytes;
        }

        public Duration getDeliveryTimeout() {
            return deliveryTimeout;
        }

        public void setDeliveryTimeout(Duration deliveryTimeout) {
            this.deliveryTimeout = deliveryTimeout;
        }

        public Duration getShutdownDrain() {
            return shutdownDrain;
        }

        public void setShutdownDrain(Duration shutdownDrain) {
            this.shutdownDrain = shutdownDrain;
        }
    }

    public static class Security {

        private List<String> trustedProxyCidrs = new ArrayList<>();

        public List<String> getTrustedProxyCidrs() {
            return trustedProxyCidrs;
        }

        public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) {
            this.trustedProxyCidrs = trustedProxyCidrs;
        }
    }
}
