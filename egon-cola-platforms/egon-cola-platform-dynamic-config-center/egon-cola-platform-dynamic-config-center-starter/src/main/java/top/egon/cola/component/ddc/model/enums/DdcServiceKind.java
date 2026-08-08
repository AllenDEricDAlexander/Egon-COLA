package top.egon.cola.component.ddc.model.enums;

/**
 * 注册中心服务类型。
 * / Registry service kind.
 */
public enum DdcServiceKind {

    /** RPC 服务提供方。 / RPC service provider. */
    RPC_PROVIDER,

    /** HTTP 服务提供方。 / HTTP service provider. */
    HTTP_PROVIDER,

    /** 内部网关服务。 / Internal gateway service. */
    INTERNAL_GATEWAY;

    /**
     * 返回该服务类型对应的租约角色。
     * / Returns the lease role represented by this service kind.
     *
     * @return 对应的租约角色 / corresponding lease role
     */
    public DdcLeaseRole leaseRole() {
        return DdcLeaseRole.valueOf(name());
    }
}
