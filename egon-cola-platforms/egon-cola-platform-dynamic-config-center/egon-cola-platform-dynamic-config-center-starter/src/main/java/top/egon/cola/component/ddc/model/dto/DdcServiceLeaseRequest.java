package top.egon.cola.component.ddc.model.dto;

import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

public class DdcServiceLeaseRequest {

    private String env;

    private String namespace;

    private DdcServiceKind serviceKind;

    private DdcServiceKey serviceKey;

    private String instanceId;

    private String leaseId;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public DdcServiceKind getServiceKind() {
        return serviceKind;
    }

    public void setServiceKind(DdcServiceKind serviceKind) {
        this.serviceKind = serviceKind;
    }

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
