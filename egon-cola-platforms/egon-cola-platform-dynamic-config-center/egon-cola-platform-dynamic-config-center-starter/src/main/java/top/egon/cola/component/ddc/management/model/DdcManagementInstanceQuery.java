package top.egon.cola.component.ddc.management.model;

/**
 * 配置客户端实例的作用域查询条件。 / Scope filters for configuration-client instances.
 *
 * @param bizCode 业务编码，可为空以不按该项过滤 / business code, nullable to omit this filter
 * @param env     环境编码，可为空以不按该项过滤 / environment code, nullable to omit this filter
 * @param appCode 应用编码，可为空以不按该项过滤 / application code, nullable to omit this filter
 */
public record DdcManagementInstanceQuery(
        String bizCode,
        String env,
        String appCode
) {
}
