package top.egon.cola.component.tianshu.state;

import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.error.DdcException;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按租户和租约标识索引当前客户端进程创建的服务注册。
 * / Indexes service registrations created by the current client process by tenant and lease identifier.
 */
public final class DdcActiveRegistrationIndex {

    private final long tenantId;

    /**
     * 当前活跃注册，键为租约标识。 / Active registrations keyed by lease identifier.
     */
    private final Map<TenantLeaseKey, ActiveRegistration> registrations = new ConcurrentHashMap<>();

    public DdcActiveRegistrationIndex(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        this.tenantId = tenantId;
    }

    /**
     * 记录成功注册的服务租约。
     * / Records a successfully registered service lease.
     *
     * @param serviceKey 注册使用的服务键 / service key used for registration
     * @param session    Admin 签发的租约会话 / lease session issued by Admin
     */
    public void put(DdcServiceKey serviceKey, DdcLeaseSession session) {
        registrations.put(new TenantLeaseKey(tenantId, session.leaseId()),
                new ActiveRegistration(serviceKey, session.instanceId()));
    }

    /**
     * 校验实例与租约属于当前活跃注册并返回其服务键。
     * / Validates that the instance and lease identify an active registration and returns its service key.
     *
     * @param instanceId 实例标识 / instance identifier
     * @param leaseId    租约标识 / lease identifier
     * @return 注册时使用的服务键 / service key used for registration
     * @throws DdcException 租约不存在或实例不匹配时抛出 / if the lease is absent or the instance does not match
     */
    public DdcServiceKey require(String instanceId, String leaseId) {
        ActiveRegistration registration = registrations.get(new TenantLeaseKey(tenantId, leaseId));
        if (registration == null || !registration.instanceId().equals(instanceId)) {
            throw new DdcException(DdcErrorStatus.LEASE_MISMATCH);
        }
        return registration.serviceKey();
    }

    /**
     * 移除指定租约的本地注册记录。
     * / Removes the local registration record for a lease.
     *
     * @param leaseId 租约标识 / lease identifier
     */
    public void remove(String leaseId) {
        registrations.remove(new TenantLeaseKey(tenantId, leaseId));
    }

    /**
     * 清空全部本地注册记录。 / Clears all local registration records.
     */
    public void clear() {
        registrations.clear();
    }

    /**
     * 本地活跃注册的最小身份信息。
     * / Minimal identity of a locally active registration.
     *
     * @param serviceKey 注册使用的服务键 / service key used for registration
     * @param instanceId 注册实例标识 / registered instance identifier
     */
    private record ActiveRegistration(DdcServiceKey serviceKey, String instanceId) {
    }

    private record TenantLeaseKey(long tenantId, String leaseId) {
    }
}
