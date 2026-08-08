package top.egon.cola.component.ddc.management.model;

/**
 * 命名空间、环境与应用之间的 DDC 作用域绑定。 /
 * DDC scope binding among a namespace, environment, and application.
 *
 * @param bindingId 绑定标识 / binding identifier
 * @param bizCode 业务编码 / business code
 * @param namespaceCode 命名空间编码 / namespace code
 * @param env 环境编码 / environment code
 * @param appId 应用内部标识 / internal application identifier
 * @param appCode 应用编码 / application code
 * @param appName 应用名称 / application name
 * @param enabled 绑定是否启用 / whether the binding is enabled
 */
public record DdcManagementScopeBinding(
        String bindingId,
        String bizCode,
        String namespaceCode,
        String env,
        String appId,
        String appCode,
        String appName,
        boolean enabled
) {
}
