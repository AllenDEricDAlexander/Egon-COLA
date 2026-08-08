package top.egon.cola.component.ddc.management.model;

/**
 * DDC 服务注册表中的规范化服务键。 / Canonical service key in the DDC service registry.
 *
 * @param bizCode 业务编码 / business code
 * @param env 环境编码 / environment code
 * @param appCode 应用编码 / application code
 * @param serviceId 服务的规范物理标识 / canonical physical service identifier
 * @param serviceKind 服务类型 / service kind
 * @param serviceName 服务名称 / service name
 * @param group 服务分组 / service group
 * @param version 服务版本 / service version
 * @param protocol 服务协议 / service protocol
 */
public record DdcManagementServiceKey(
        String bizCode,
        String env,
        String appCode,
        String serviceId,
        String serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) {
}
