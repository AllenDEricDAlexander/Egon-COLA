package top.egon.cola.component.ddc.model.enums;

public enum DdcServiceKind {

    RPC_PROVIDER,

    HTTP_PROVIDER,

    INTERNAL_GATEWAY;

    public DdcLeaseRole leaseRole() {
        return DdcLeaseRole.valueOf(name());
    }
}
