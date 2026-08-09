package top.egon.cola.component.ddc.http.registration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(DdcHttpRegistrationProperties.PREFIX)
public class DdcHttpRegistrationProperties {

    public static final String PREFIX =
            "egon.cola.component.ddc.registry.http";

    private boolean enabled;

    private String env;

    private String namespace;

    private String instanceId;

    private String serviceName;

    private String group = "default";

    private String version;

    private String protocol = "http";

    private String advertisedHost;

    private int port;

    private int leaseSeconds = 30;

    private int heartbeatIntervalSeconds = 10;

    private boolean failFast = true;

    private Map<String, String> metadata = new LinkedHashMap<>();

    public DdcHttpRegistrationRuntimeProperties toRuntime(
            String contributedVersion,
            Map<String, String> contributedMetadata,
            int actualPort) {
        if (contributedVersion != null
                && version != null
                && !version.trim().equals(contributedVersion.trim())) {
            throw new IllegalArgumentException(
                    "HTTP registration version conflicts with contributor"
            );
        }
        String runtimeVersion = version == null || version.isBlank()
                ? contributedVersion
                : version;
        int runtimePort = port == 0 && actualPort > 0
                ? actualPort
                : port;
        return new DdcHttpRegistrationRuntimeProperties(
                enabled,
                env,
                namespace,
                instanceId,
                serviceName,
                group,
                runtimeVersion,
                protocol,
                advertisedHost,
                runtimePort,
                leaseSeconds,
                heartbeatIntervalSeconds,
                failFast,
                mergedMetadata(contributedMetadata)
        );
    }

    private Map<String, String> mergedMetadata(
            Map<String, String> contributedMetadata) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(metadata);
        if (contributedMetadata != null) {
            contributedMetadata.forEach((key, value) -> {
                String previous = merged.putIfAbsent(key, value);
                if (previous != null && !previous.equals(value)) {
                    throw new IllegalArgumentException(
                            "HTTP registration metadata conflicts: " + key
                    );
                }
            });
        }
        return Map.copyOf(merged);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAdvertisedHost() {
        return advertisedHost;
    }

    public void setAdvertisedHost(String advertisedHost) {
        this.advertisedHost = advertisedHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(metadata);
    }
}
