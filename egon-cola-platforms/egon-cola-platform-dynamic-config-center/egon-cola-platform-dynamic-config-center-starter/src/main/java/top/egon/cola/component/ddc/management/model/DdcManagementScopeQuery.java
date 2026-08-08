package top.egon.cola.component.ddc.management.model;

/**
 * DDC 作用域绑定的查询条件。 / Query filters for DDC scope bindings.
 *
 * @param bizCode       业务编码，可为空以不按该项过滤 / business code, nullable to omit this filter
 * @param namespaceCode 命名空间编码，可为空以不按该项过滤 / namespace code, nullable to omit this filter
 * @param env           环境编码，可为空以不按该项过滤 / environment code, nullable to omit this filter
 * @param appCode       应用编码，可为空以不按该项过滤 / application code, nullable to omit this filter
 */
public record DdcManagementScopeQuery(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode
) {
}
