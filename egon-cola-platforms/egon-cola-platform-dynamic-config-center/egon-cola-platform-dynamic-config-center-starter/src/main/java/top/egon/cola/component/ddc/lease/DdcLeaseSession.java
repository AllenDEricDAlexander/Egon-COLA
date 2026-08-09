package top.egon.cola.component.ddc.lease;

import top.egon.cola.component.ddc.lease.DdcLeaseRole;

import java.time.Instant;

/**
 * Admin 为已注册实例签发的租约会话。
 * / Lease session issued by Admin for a registered instance.
 *
 * @param instanceId               实例标识 / instance identifier
 * @param leaseId                  租约标识 / lease identifier
 * @param role                     租约持有方角色 / lease holder role
 * @param leaseSeconds             租约有效期秒数 / lease duration in seconds
 * @param heartbeatIntervalSeconds 建议的心跳间隔秒数 / recommended heartbeat interval in seconds
 * @param registeredAt             注册时间 / registration time
 * @param leaseExpireAt            租约到期时间 / lease expiration time
 */
public record DdcLeaseSession(
        String instanceId,
        String leaseId,
        DdcLeaseRole role,
        int leaseSeconds,
        int heartbeatIntervalSeconds,
        Instant registeredAt,
        Instant leaseExpireAt
) {
}
