package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.management.model.DdcInstanceStatus;

import java.time.Instant;
import java.util.Map;

public record DdcServiceInstance(
        String instanceId,
        String leaseId,
        DdcServiceKey serviceKey,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata,
        int leaseSeconds,
        int heartbeatIntervalSeconds,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant leaseExpireAt,
        String status,
        long revision
) implements Comparable<DdcServiceInstance> {

    public DdcServiceInstance {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId is required");
        }
        if (leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException("leaseId is required");
        }
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        metadata = DdcServiceRegistration.validatedMetadata(metadata);
    }

    public DdcInstanceStatus normalizedStatus() {
        return DdcInstanceStatus.fromWire(status);
    }

    @Override
    public int compareTo(DdcServiceInstance other) {
        int instanceOrder = instanceId.compareTo(other.instanceId);
        return instanceOrder == 0 ? leaseId.compareTo(other.leaseId) : instanceOrder;
    }
}
