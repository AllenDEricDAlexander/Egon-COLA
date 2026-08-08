package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.Map;

/**
 * 管理接口返回的配置客户端租约实例。 / Configuration-client lease instance returned by the management API.
 *
 * @param bizCode 业务编码 / business code
 * @param env 环境编码 / environment code
 * @param appCode 应用编码 / application code
 * @param instanceId 实例标识 / instance identifier
 * @param leaseId 租约标识 / lease identifier
 * @param host 实例主机 / instance host
 * @param port 实例端口 / instance port
 * @param leaseRole 租约角色 / lease role
 * @param status 服务端租约状态文本 / server-side lease-status text
 * @param registeredAt 注册时间 / registration time
 * @param lastHeartbeatAt 最近心跳时间 / most recent heartbeat time
 * @param expireAt 租约过期时间 / lease expiration time
 * @param metadata 配置客户端元数据 / configuration-client metadata
 */
public record DdcManagementConfigClientInstance(
        String bizCode,
        String env,
        String appCode,
        String instanceId,
        String leaseId,
        String host,
        Integer port,
        String leaseRole,
        String status,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant expireAt,
        Map<String, String> metadata
) {

    /**
     * 构造配置客户端实例并将元数据归一化为不可变映射。 /
     * Constructs a configuration-client instance and normalizes metadata to an immutable map.
     */
    public DdcManagementConfigClientInstance {
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
