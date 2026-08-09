package top.egon.cola.component.ddc.lease;

/**
 * DDC 租约持有方角色。
 * / Role of a DDC lease holder.
 */
public enum DdcLeaseRole {

    /**
     * 配置客户端实例。 / Configuration client instance.
     */
    CONFIG_CLIENT,

    /**
     * RPC 服务提供方实例。 / RPC service provider instance.
     */
    RPC_PROVIDER,

    /**
     * HTTP 服务提供方实例。 / HTTP service provider instance.
     */
    HTTP_PROVIDER,

    /**
     * 内部网关实例。 / Internal gateway instance.
     */
    INTERNAL_GATEWAY
}
