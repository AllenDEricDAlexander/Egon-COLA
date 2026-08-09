package top.egon.cola.component.ddc.model.instance;

import org.springframework.lang.Nullable;

/**
 * 配置客户端实例的物理身份与运行端点信息。
 * / Physical identity and runtime endpoint of a configuration client instance.
 *
 * @param instanceId 实例标识 / instance identifier
 * @param bizCode    业务编码 / business code
 * @param appCode    应用编码 / application code
 * @param env        运行环境 / runtime environment
 * @param host       实例主机地址 / instance host address
 * @param port       实例端口 / instance port
 * @param pid        进程标识 / process identifier
 * @param sdkVersion SDK 版本 / SDK version
 */
public record DdcInstanceIdentity(
        String instanceId,
        String bizCode,
        String appCode,
        String env,
        String host,
        @Nullable Integer port,
        String pid,
        String sdkVersion
) {

    /**
     * 返回已移除的 namespace 兼容视图；该值不再属于物理实例身份。
     * / Returns the removed namespace compatibility view; it is no longer part
     * of the physical instance identity.
     *
     * @return 始终为 {@code null} / always {@code null}
     * @deprecated namespace 仅是可见性视图，不属于物理实例身份。
     * / namespace is only a visibility view and is not part of physical instance identity.
     */
    @Deprecated(forRemoval = true)
    @Nullable
    public String namespace() {
        return null;
    }
}
