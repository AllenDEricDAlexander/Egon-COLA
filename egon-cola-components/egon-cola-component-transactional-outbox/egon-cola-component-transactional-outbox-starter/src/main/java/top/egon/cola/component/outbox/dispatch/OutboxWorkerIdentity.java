package top.egon.cola.component.outbox.dispatch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class OutboxWorkerIdentity {

    private final String nodeId;

    public OutboxWorkerIdentity(String configuredNodeId) {
        this.nodeId = configuredNodeId == null || configuredNodeId.isBlank()
                ? generatedNodeId()
                : sanitize(configuredNodeId);
    }

    public String nodeId() {
        return nodeId;
    }

    public String nextLeaseOwner() {
        return nodeId + ":" + UUID.randomUUID();
    }

    private String generatedNodeId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            host = "unknown";
        }
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return sanitize(host) + "-" + ProcessHandle.current().pid() + "-" + suffix;
    }

    private String sanitize(String value) {
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }
}
