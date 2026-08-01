package top.egon.cola.component.ddc.model.dto;

import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

public class DdcServiceLeaseRequest {

    private DdcServiceKey serviceKey;

    private String instanceId;

    private String leaseId;

    public DdcServiceKey getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(DdcServiceKey serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }
}
