package top.egon.cola.component.gateway.engine.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Validated runtime limits for MCP transports and optional capabilities.
 */
@ConfigurationProperties(prefix = "egon.cola.component.gateway.engine.mcp")
public class McpRuntimeProperties {

    private boolean enabled = true;

    private String artifactRoot = System.getProperty("java.io.tmpdir")
            + "/egon-cola/gateway-mcp-artifacts";

    private Duration sessionTtl = Duration.ofMinutes(30);

    private Duration streamWait = Duration.ofSeconds(15);

    private long maximumRequestBytes = 16L * 1024 * 1024;

    private long maximumResourceBytes = 64L * 1024 * 1024;

    private long maximumAppArtifactBytes = 16L * 1024 * 1024;

    private int maximumSubscriptionsPerClient = 100;

    private int maximumActiveTasksPerClient = 100;

    private final Tasks tasks = new Tasks();

    private final Remote remote = new Remote();

    private final Security security = new Security();

    private final Audit audit = new Audit();

    public void validate() {
        artifactRoot = required(artifactRoot, "artifactRoot");
        sessionTtl = positive(sessionTtl, "sessionTtl");
        streamWait = positive(streamWait, "streamWait");
        maximumRequestBytes = size(
                maximumRequestBytes,
                "maximumRequestBytes"
        );
        maximumResourceBytes = size(
                maximumResourceBytes,
                "maximumResourceBytes"
        );
        maximumAppArtifactBytes = size(
                maximumAppArtifactBytes,
                "maximumAppArtifactBytes"
        );
        if (maximumAppArtifactBytes > maximumResourceBytes) {
            throw new IllegalArgumentException(
                    "MCP maximumAppArtifactBytes exceeds resource limit"
            );
        }
        bounded(
                maximumSubscriptionsPerClient,
                "maximumSubscriptionsPerClient"
        );
        bounded(maximumActiveTasksPerClient, "maximumActiveTasksPerClient");
        tasks.validate();
        remote.validate();
        security.validate();
        audit.validate();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getArtifactRoot() {
        return artifactRoot;
    }

    public void setArtifactRoot(String artifactRoot) {
        this.artifactRoot = artifactRoot;
    }

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public Duration getStreamWait() {
        return streamWait;
    }

    public void setStreamWait(Duration streamWait) {
        this.streamWait = streamWait;
    }

    public long getMaximumRequestBytes() {
        return maximumRequestBytes;
    }

    public void setMaximumRequestBytes(long maximumRequestBytes) {
        this.maximumRequestBytes = maximumRequestBytes;
    }

    public long getMaximumResourceBytes() {
        return maximumResourceBytes;
    }

    public void setMaximumResourceBytes(long maximumResourceBytes) {
        this.maximumResourceBytes = maximumResourceBytes;
    }

    public long getMaximumAppArtifactBytes() {
        return maximumAppArtifactBytes;
    }

    public void setMaximumAppArtifactBytes(long maximumAppArtifactBytes) {
        this.maximumAppArtifactBytes = maximumAppArtifactBytes;
    }

    public int getMaximumSubscriptionsPerClient() {
        return maximumSubscriptionsPerClient;
    }

    public void setMaximumSubscriptionsPerClient(
            int maximumSubscriptionsPerClient) {
        this.maximumSubscriptionsPerClient = maximumSubscriptionsPerClient;
    }

    public int getMaximumActiveTasksPerClient() {
        return maximumActiveTasksPerClient;
    }

    public void setMaximumActiveTasksPerClient(
            int maximumActiveTasksPerClient) {
        this.maximumActiveTasksPerClient = maximumActiveTasksPerClient;
    }

    public Tasks getTasks() {
        return tasks;
    }

    public Remote getRemote() {
        return remote;
    }

    public Security getSecurity() {
        return security;
    }

    public Audit getAudit() {
        return audit;
    }

    public static class Tasks {

        private Duration leaseDuration = Duration.ofSeconds(30);

        private Duration pollInterval = Duration.ofSeconds(2);

        private Duration defaultTtl = Duration.ofHours(24);

        private Duration cleanupInterval = Duration.ofMinutes(10);

        private void validate() {
            leaseDuration = positive(leaseDuration, "tasks.leaseDuration");
            pollInterval = positive(pollInterval, "tasks.pollInterval");
            defaultTtl = positive(defaultTtl, "tasks.defaultTtl");
            cleanupInterval = positive(
                    cleanupInterval,
                    "tasks.cleanupInterval"
            );
            if (pollInterval.compareTo(leaseDuration) >= 0) {
                throw new IllegalArgumentException(
                        "MCP task pollInterval must be shorter than leaseDuration"
                );
            }
        }

        public Duration getLeaseDuration() {
            return leaseDuration;
        }

        public void setLeaseDuration(Duration leaseDuration) {
            this.leaseDuration = leaseDuration;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getCleanupInterval() {
            return cleanupInterval;
        }

        public void setCleanupInterval(Duration cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
        }
    }

    public static class Remote {

        private Duration discoveryTimeout = Duration.ofSeconds(20);

        private Duration callTimeout = Duration.ofSeconds(60);

        private Duration healthInterval = Duration.ofSeconds(30);

        private Duration capabilitySyncInterval = Duration.ofMinutes(5);

        private Duration circuitOpenDuration = Duration.ofSeconds(30);

        private int maximumConcurrentCalls = 32;

        private int failureThreshold = 3;

        private boolean tokenForwarding;

        private void validate() {
            discoveryTimeout = positive(
                    discoveryTimeout,
                    "remote.discoveryTimeout"
            );
            callTimeout = positive(callTimeout, "remote.callTimeout");
            healthInterval = positive(
                    healthInterval,
                    "remote.healthInterval"
            );
            capabilitySyncInterval = positive(
                    capabilitySyncInterval,
                    "remote.capabilitySyncInterval"
            );
            circuitOpenDuration = positive(
                    circuitOpenDuration,
                    "remote.circuitOpenDuration"
            );
            bounded(maximumConcurrentCalls, "remote.maximumConcurrentCalls");
            bounded(failureThreshold, "remote.failureThreshold");
            if (tokenForwarding) {
                throw new IllegalArgumentException(
                        "MCP remote token forwarding must remain disabled"
                );
            }
        }

        public Duration getDiscoveryTimeout() {
            return discoveryTimeout;
        }

        public void setDiscoveryTimeout(Duration discoveryTimeout) {
            this.discoveryTimeout = discoveryTimeout;
        }

        public Duration getCallTimeout() {
            return callTimeout;
        }

        public void setCallTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
        }

        public Duration getHealthInterval() {
            return healthInterval;
        }

        public void setHealthInterval(Duration healthInterval) {
            this.healthInterval = healthInterval;
        }

        public Duration getCapabilitySyncInterval() {
            return capabilitySyncInterval;
        }

        public void setCapabilitySyncInterval(
                Duration capabilitySyncInterval) {
            this.capabilitySyncInterval = capabilitySyncInterval;
        }

        public Duration getCircuitOpenDuration() {
            return circuitOpenDuration;
        }

        public void setCircuitOpenDuration(Duration circuitOpenDuration) {
            this.circuitOpenDuration = circuitOpenDuration;
        }

        public int getMaximumConcurrentCalls() {
            return maximumConcurrentCalls;
        }

        public void setMaximumConcurrentCalls(int maximumConcurrentCalls) {
            this.maximumConcurrentCalls = maximumConcurrentCalls;
        }

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public boolean isTokenForwarding() {
            return tokenForwarding;
        }

        public void setTokenForwarding(boolean tokenForwarding) {
            this.tokenForwarding = tokenForwarding;
        }
    }

    public static class Security {

        private boolean originValidation = true;

        private boolean protectedResourceMetadata = true;

        private boolean tokenForwarding;

        private void validate() {
            if (tokenForwarding) {
                throw new IllegalArgumentException(
                        "MCP security token forwarding must remain disabled"
                );
            }
        }

        public boolean isOriginValidation() {
            return originValidation;
        }

        public void setOriginValidation(boolean originValidation) {
            this.originValidation = originValidation;
        }

        public boolean isProtectedResourceMetadata() {
            return protectedResourceMetadata;
        }

        public void setProtectedResourceMetadata(
                boolean protectedResourceMetadata) {
            this.protectedResourceMetadata = protectedResourceMetadata;
        }

        public boolean isTokenForwarding() {
            return tokenForwarding;
        }

        public void setTokenForwarding(boolean tokenForwarding) {
            this.tokenForwarding = tokenForwarding;
        }
    }

    public static class Audit {

        private boolean enabled = true;

        private boolean bodyLogEnabled;

        private void validate() {
            if (bodyLogEnabled) {
                throw new IllegalArgumentException(
                        "MCP audit body logging must remain disabled"
                );
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBodyLogEnabled() {
            return bodyLogEnabled;
        }

        public void setBodyLogEnabled(boolean bodyLogEnabled) {
            this.bodyLogEnabled = bodyLogEnabled;
        }
    }

    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "MCP " + field + " must be positive"
            );
        }
        return value;
    }

    private static long size(long value, String field) {
        if (value < 1L || value > 1024L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "MCP " + field + " is outside its safe range"
            );
        }
        return value;
    }

    private static void bounded(int value, String field) {
        if (value < 1 || value > 10_000) {
            throw new IllegalArgumentException(
                    "MCP " + field + " is outside its safe range"
            );
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MCP " + field + " is required");
        }
        return value.trim();
    }
}
