package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.Map;

/**
 * 管理接口返回的服务注册实例。 / Service-registry instance returned by the management API.
 *
 * @param instanceId 实例标识 / instance identifier
 * @param leaseId 租约标识 / lease identifier
 * @param host 实例主机 / instance host
 * @param port 实例端口 / instance port
 * @param secure 是否使用安全传输 / whether secure transport is used
 * @param metadata 实例元数据 / instance metadata
 * @param status 服务端租约状态文本 / server-side lease-status text
 * @param registeredAt 注册时间 / registration time
 * @param lastHeartbeatAt 最近心跳时间 / most recent heartbeat time
 * @param expireAt 租约过期时间 / lease expiration time
 */
public record DdcManagementServiceInstance(
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata,
        String status,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant expireAt
) {

    /**
     * 构造服务实例并将元数据归一化为不可变映射。 /
     * Constructs a service instance and normalizes metadata to an immutable map.
     */
    public DdcManagementServiceInstance {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 将服务端租约状态归一化为稳定的客户端枚举。 / Normalizes the server lease status to a stable client enum.
     *
     * @return 归一化租约状态 / normalized lease status
     */
    public DdcInstanceStatus normalizedStatus() {
        return DdcInstanceStatus.fromWire(status);
    }
}
