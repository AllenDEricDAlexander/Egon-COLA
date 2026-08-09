package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.model.management.DdcInstanceStatus;

import java.time.Instant;
import java.util.Map;

/**
 * 注册中心中的服务实例及其当前租约状态。
 * / Service instance and its current lease state in the registry.
 *
 * @param instanceId               实例标识 / instance identifier
 * @param leaseId                  租约标识 / lease identifier
 * @param serviceKey               服务键 / service key
 * @param host                     实例主机地址 / instance host address
 * @param port                     服务端口 / service port
 * @param secure                   是否使用安全传输 / whether secure transport is used
 * @param metadata                 不可变的实例元数据 / immutable instance metadata
 * @param leaseSeconds             租约有效期秒数 / lease duration in seconds
 * @param heartbeatIntervalSeconds 心跳间隔秒数 / heartbeat interval in seconds
 * @param registeredAt             注册时间 / registration time
 * @param lastHeartbeatAt          最近一次心跳时间 / most recent heartbeat time
 * @param leaseExpireAt            租约到期时间 / lease expiration time
 * @param status                   实例状态的线协议值 / wire value of the instance status
 * @param revision                 实例记录修订号 / instance record revision
 */
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

    /**
     * 校验实例、租约与服务身份，并规范化元数据。
     * / Validates instance, lease, and service identity and normalizes metadata.
     *
     * @throws IllegalArgumentException 必填身份缺失或元数据无效时抛出
     *                                  / if a required identity is missing or metadata is invalid
     */
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

    /**
     * 将线协议状态转换为统一实例状态。
     * / Converts the wire status to the normalized instance status.
     *
     * @return 统一实例状态 / normalized instance status
     */
    public DdcInstanceStatus normalizedStatus() {
        return DdcInstanceStatus.fromWire(status);
    }

    /**
     * 按实例标识、再按租约标识比较实例。
     * / Compares instances by instance identifier and then lease identifier.
     *
     * @param other 待比较实例 / instance to compare with
     * @return 负数、零或正数，分别表示小于、等于或大于
     * / negative, zero, or positive when less than, equal to, or greater than
     */
    @Override
    public int compareTo(DdcServiceInstance other) {
        int instanceOrder = instanceId.compareTo(other.instanceId);
        return instanceOrder == 0 ? leaseId.compareTo(other.leaseId) : instanceOrder;
    }
}
