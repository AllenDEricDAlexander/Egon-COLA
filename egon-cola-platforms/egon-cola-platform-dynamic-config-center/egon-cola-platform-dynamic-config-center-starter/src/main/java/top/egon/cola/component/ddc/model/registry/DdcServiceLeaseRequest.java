package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

/**
 * 服务实例租约续期或注销请求。
 * / Service instance lease renewal or deregistration request.
 */
public class DdcServiceLeaseRequest {

    /**
     * 租约所属服务键。 / Service key that owns the lease.
     */
    private DdcServiceKey serviceKey;

    /**
     * 实例标识。 / Instance identifier.
     */
    private String instanceId;

    /**
     * 租约标识。 / Lease identifier.
     */
    private String leaseId;

    /**
     * 返回租约所属服务键。 / Returns the service key that owns the lease.
     *
     * @return 服务键 / service key
     */
    public DdcServiceKey getServiceKey() {
        return serviceKey;
    }

    /**
     * 设置租约所属服务键。 / Sets the service key that owns the lease.
     *
     * @param serviceKey 服务键 / service key
     */
    public void setServiceKey(DdcServiceKey serviceKey) {
        this.serviceKey = serviceKey;
    }

    /**
     * 返回实例标识。 / Returns the instance identifier.
     *
     * @return 实例标识 / instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 设置实例标识。 / Sets the instance identifier.
     *
     * @param instanceId 实例标识 / instance identifier
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 返回租约标识。 / Returns the lease identifier.
     *
     * @return 租约标识 / lease identifier
     */
    public String getLeaseId() {
        return leaseId;
    }

    /**
     * 设置租约标识。 / Sets the lease identifier.
     *
     * @param leaseId 租约标识 / lease identifier
     */
    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }
}
