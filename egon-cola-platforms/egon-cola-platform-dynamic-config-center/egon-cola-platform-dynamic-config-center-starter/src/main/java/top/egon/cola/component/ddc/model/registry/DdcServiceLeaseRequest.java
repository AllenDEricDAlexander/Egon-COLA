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

    /** Opaque IdP SERVICE access token used for heartbeat validation. */
    private String registrationToken;

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

    /**
     * 返回心跳 SERVICE Token。 / Returns the heartbeat SERVICE token.
     *
     * @return 不透明 SERVICE Token / opaque SERVICE token
     */
    public String getRegistrationToken() {
        return registrationToken;
    }

    /**
     * 设置心跳 SERVICE Token。 / Sets the heartbeat SERVICE token.
     *
     * @param registrationToken 不透明 SERVICE Token / opaque SERVICE token
     */
    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = DdcServiceRegistration
                .requireRegistrationToken(registrationToken);
    }

    /**
     * 返回不会泄漏原始 registration token 的租约诊断文本。
     * / Returns lease diagnostic text that never exposes the raw registration token.
     *
     * @return 已脱敏租约摘要 / redacted lease summary
     */
    @Override
    public String toString() {
        return "DdcServiceLeaseRequest[serviceKey=" + serviceKey
                + ", instanceId=" + instanceId
                + ", leaseId=" + leaseId
                + ", registrationToken=<redacted>]";
    }
}
